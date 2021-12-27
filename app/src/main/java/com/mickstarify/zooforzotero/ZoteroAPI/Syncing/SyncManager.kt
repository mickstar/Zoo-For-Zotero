package com.mickstarify.zooforzotero.ZoteroAPI.Syncing

import android.os.Bundle
import android.util.Log
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.ZoteroAPI.APIKeyRevokedException
import com.mickstarify.zooforzotero.ZoteroAPI.LibraryVersionMisMatchException
import com.mickstarify.zooforzotero.ZoteroAPI.Model.CollectionPOJO
import com.mickstarify.zooforzotero.ZoteroAPI.UpToDateException
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroAPI
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroAPIItemsResponse
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ItemsDownloadProgress
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroDB
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Action
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class SyncManager (
    val zoteroAPI: ZoteroAPI,
    val syncChangeListener: OnSyncChangeListener,
){

    // these three variables keep track of our download progress and are used to make sure
    // all 3 downloads finish.
    var loadingItems = false
    var loadingCollections = false
    var loadingTrash = false

    @Inject
    lateinit var preferences: PreferenceManager

    @Inject
    lateinit var zoteroDatabase: ZoteroDatabase

    fun isSyncing() : Boolean {
        return (loadingItems || loadingCollections || loadingTrash)
    }

    fun startCompleteSync(zoteroDB: ZoteroDB, useSmallLoadingAnimation: Boolean) {
        /*LIbrary loading will load in 3 stages,
        *
        * Stage 1, Downloading stage
        *   Here, 3 threads will run, downloading items, collections and trash.
        *   All three threads run independently and will all need to complete before stage 2.
        *
        * Stage 2. Downloading Deleted Items
        *   Here deleted items will need to be queried. This must complete after stage 1 because
        *   there may be items that get deleted before being synced.
        *
        * Stage 3.
        *   Loading, up until now  all downloaded data has been commited to the SQL DB. This has
        *   to now be loaded to memory in ZoteroDB class.
        *
        *
        *   How each stage is reached is somewhat complex but here in this method you will find stage 1.
         */

        if (loadingCollections || loadingItems || loadingTrash) {
            Log.e("zotero", "Error, we are already loading our library! not doing it again.")
            return
        }


        // show our loading animation.
        syncChangeListener.startSyncAnimation(useSmallLoadingAnimation)

        Log.d("zotero", "initializing sync for ${zoteroDB.groupID}")
        loadItems(zoteroDB, useSmallLoadingAnimation)
        loadCollections(zoteroDB)
        loadTrashedItems(zoteroDB)
    }

    fun startItemsSync(db: ZoteroDB){
        this.loadItems(db, true)
    }

    private fun loadCollections(db: ZoteroDB) {
        loadingCollections = true
        val libraryVersion = db.getLibraryVersion()
        zoteroAPI.getCollections(libraryVersion, db.groupID).map { collectionPojos ->
            // we will write collections to the database as we receive them.
            // this will be done using the rxjava thread.
            if (collectionPojos.size > 0) {
                val collectionObjects = collectionPojos.map { Collection(it, db.groupID) }
                zoteroDatabase.writeCollections(collectionObjects).blockingAwait()
            }
            collectionPojos
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<List<CollectionPOJO>> {
                override fun onComplete() {
                    Log.d("zotero", "finished getting collections.")
                    finishGetCollections(db)
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {
                    if (e is UpToDateException) {
                        Log.d("zotero", "local copy of collections is already up to date.")
                    } else if (e is APIKeyRevokedException) {
                        syncChangeListener.createErrorAlert(
                            "Invalid API Key",
                            "Your API Key is invalid. To rectify this issue please clear app data for the app. " +
                                    "Then relogin."
                        ) {}
                    } else {
                        val bundle = Bundle()
                        bundle.putString("error_message", e.message)
                        syncChangeListener.createErrorAlert(
                            "Error downloading Collections",
                            "Message: ${e}"
                        ) {}
                    }
                    finishGetCollections(db)
                }

                override fun onNext(t: List<CollectionPOJO>) {
                    // todo add progress to collection downloading.
                }

            })
    }

    private fun loadTrashedItems(
        db: ZoteroDB
    ) {
        loadingTrash = true

        //TODO Will remove in a future version
        if (preferences.firstRunForVersion28()){
            // I have recently fixed the deleted/trash syncing, so we will need a full resync from the beginning
            // to properly have the changes reflected in Zoo.
            db.setTrashVersion(0)
            db.setLastDeletedItemsCheckVersion(0)
        }

        val observable =
            zoteroAPI.getTrashedItems(db.groupID, db.getTrashVersion()).map { response ->
                for (itemPojo in response.items) {
                    if (zoteroDatabase.containsItem(db.groupID, itemPojo.ItemKey).blockingGet()) {
                        // there is a subtle flaw here.
                        // If the item has changed since last sync, those changes won't be reflected in
                        // zoo. I will ignore this error for the sake of simplicity.
                        zoteroDatabase.moveItemToTrash(db.groupID, itemPojo.ItemKey).blockingAwait()
                    } else {
                        zoteroDatabase.writeItem(db.groupID, itemPojo).blockingAwait()
                        zoteroDatabase.moveItemToTrash(db.groupID, itemPojo.ItemKey).blockingAwait()
                    }
                }
                response
            }
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ZoteroAPIItemsResponse> {
                var received = 0
                var trashLibraryVersion = 0
                override fun onComplete() {
                    // finished syncing trash.
                    db.setTrashVersion(trashLibraryVersion)
                    finishGetTrash(db)
                }

                override fun onSubscribe(d: Disposable) {
                    Log.d("zotero", "beginning sync request for trash")
                }

                override fun onNext(response: ZoteroAPIItemsResponse) {
                    received += response.items.size
                    trashLibraryVersion = response.LastModifiedVersion
                    Log.d("zotero", "received ${received} of ${response.totalResults} trash items")
                }

                override fun onError(e: Throwable) {
                    if (e is UpToDateException) {
                        Log.d("zotero", "trashed items has not changed")
                    } else {
                        Log.e("zotero", "got error from request to /trash $e")
                    }
                    finishGetTrash(db)
                }

            })
    }

    private fun loadItems(
        db: ZoteroDB,
        useSmallLoadingAnimation: Boolean = false
    ) {
        loadingItems = true
        val progress = db.getDownloadProgress()

        val itemsObservable =
            zoteroAPI.getItems(
                db.getLibraryVersion(),
                db.groupID,
                downloadProgress = progress
            )
        itemsObservable.map { response ->
            // check to see if there are any items.
            if (response.isCached == false) {
                zoteroDatabase.writeItemPOJOs(db.groupID, response.items).blockingAwait()
            }
            response // pass it on.
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ZoteroAPIItemsResponse> {
                var libraryVersion = -1 //dummy value, will be replaced in onNext()
                var downloaded = progress?.nDownloaded ?: 0
                override fun onComplete() {
                    Log.d("zotero", "load Items oncomplete")
                    if (db.getLibraryVersion() > -1) {
                        if (downloaded == 0) {
                            syncChangeListener.makeToastAlert("Already up to date.")
                        } else {
                            syncChangeListener.makeToastAlert("Updated ${downloaded} items.")
                        }
                    }
                    db.destroyDownloadProgress()
                    db.setItemsVersion(libraryVersion)
                    db.updateDatabaseLastSyncedTimestamp()
                    finishGetItems(db)
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(response: ZoteroAPIItemsResponse) {
                    if (response.isCached == false) {
                        this.libraryVersion = response.LastModifiedVersion
                        this.downloaded += response.items.size

                        if (this.downloaded < response.totalResults)
                            db.setDownloadProgress(
                                ItemsDownloadProgress(
                                    response.LastModifiedVersion,
                                    this.downloaded,
                                    response.totalResults
                                )
                            )
                        if (!useSmallLoadingAnimation) {
                            syncChangeListener.setSyncProgress(
                                downloaded,
                                response.totalResults)
                        }
                        Log.d("zotero", "got ${this.downloaded} of ${response.totalResults} items")
                    } else {
                        Log.d("zotero", "got back cached response for items.")
                    }
                }

                override fun onError(e: Throwable) {
                    if (e is UpToDateException) {
                        Log.d("zotero", "our items are already up to date.")
                        db.updateDatabaseLastSyncedTimestamp()
                    } else if (e is LibraryVersionMisMatchException) {
                        // we need to redownload items but without the progress.
                        Log.d("zotero", "mismatched, reloading items.")
                        db.destroyDownloadProgress()
                        syncChangeListener.makeToastAlert("Could not continue, library has changed since last sync.")
                        loadItems(db, useSmallLoadingAnimation = false)
                        return
                    } else {
                        syncChangeListener.createErrorAlert("Error downloading items", "message: ${e}", {})
                        Log.e("zotero", "${e}")
                        Log.e("zotero", "${e.stackTrace}")
                    }
                    finishGetItems(db)
                }
            })
    }

    private fun finishGetCollections(db: ZoteroDB) {
        loadingCollections = false
        if (!loadingItems && !loadingTrash) {
            loadLibraryStage2(db)
        }
    }

    private fun finishGetItems(db: ZoteroDB) {
        loadingItems = false
        if (!loadingCollections && !loadingTrash) {
            loadLibraryStage2(db)
        }
    }

    private fun finishGetTrash(db: ZoteroDB) {
        loadingTrash = false
        if (!loadingItems && !loadingCollections) {
            loadLibraryStage2(db)
        }
    }

    fun loadLibraryStage2(db: ZoteroDB) {
        // as  defined above, stage 2 handles deleted files.
        if (loadingTrash || loadingCollections || loadingItems) {
            throw Exception("Error cannot proceed to stage 2 if library still loading.")
        }

        updateDeletedEntries(db)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object :
                CompletableObserver {
                override fun onSubscribe(d: Disposable) {
                    // nothing.
                }

                override fun onComplete() {
                    syncChangeListener.finishLibrarySync(db)
                }

                override fun onError(e: Throwable) {
                Log.e("zotero", "there was an error processing deleted Entries, ${e}")
                syncChangeListener.createErrorAlert(
                    "Error Updating Deleted Items",
                    "message: ${e}",
                    {})
            }
        })


    }

    fun updateDeletedEntries(db: ZoteroDB): Completable {
        /* Checks for deleted entries on the zotero servers and mirrors those changes on the local database. */
        // we have to assume the library is loaded.

        val deletedItemsCheckVersion = db.getLastDeletedItemsCheckVersion()
        val libraryVersion = db.getLibraryVersion()
        if (deletedItemsCheckVersion == libraryVersion) {
            Log.d(
                "zotero",
                "not checking deleted items because library hasn't changed. ${libraryVersion}"
            )
            return Completable.complete() // our job is done, there is nothing to check.
        }

        val completable = Completable.fromAction(Action {
            val deletedEntriesPojo = zoteroAPI.getDeletedEntries(deletedItemsCheckVersion, db.groupID).blockingGet()
            for (itemKey in deletedEntriesPojo.items){
                zoteroDatabase.deleteItem(itemKey).blockingGet()
            }
            for (collectionKey in deletedEntriesPojo.collections){
                zoteroDatabase.deleteCollection(collectionKey).blockingGet()
            }

            Log.d(
                "zotero",
                "Setting deletedLibraryVersion to $libraryVersion from $deletedItemsCheckVersion"
            )
            db.setLastDeletedItemsCheckVersion(libraryVersion)
        })

        return completable
    }
}