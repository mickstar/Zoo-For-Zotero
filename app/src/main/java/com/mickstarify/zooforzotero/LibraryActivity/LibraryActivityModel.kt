package com.mickstarify.zooforzotero.LibraryActivity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.JsonObject
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import com.mickstarify.zooforzotero.ZooForZoteroApplication
import com.mickstarify.zooforzotero.ZoteroAPI.*
import com.mickstarify.zooforzotero.ZoteroAPI.Model.CollectionPOJO
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.*
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ItemsDownloadProgress
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroDB
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroDBPicker
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroGroupDB
import io.reactivex.*
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class LibraryActivityModel(private val presenter: Contract.Presenter, val context: Context) :
    Contract.Model {

    private var collectionsByMenuId: HashMap<Int, String>? = null
    // stores the current item being viewed by the user. (useful for refreshing the view)
    var selectedItem: Item? = null
    var isDisplayingItems = false

    private var firebaseAnalytics: FirebaseAnalytics

    private lateinit var zoteroAPI: ZoteroAPI
    @Inject
    lateinit var zoteroDatabase: ZoteroDatabase
    @Inject
    lateinit var attachmentStorageManager: AttachmentStorageManager
    private val zoteroGroupDB =
        ZoteroGroupDB(
            context
        )
    private var zoteroDBPicker =
        ZoteroDBPicker(
            ZoteroDB(
                context,
                groupID = -1
            ), zoteroGroupDB
        )
    private var groups: List<GroupInfo>? = null

    @Inject
    lateinit var preferences: PreferenceManager

    val state = LibraryModelState()

    private var zoteroDB: ZoteroDB by zoteroDBPicker

    // these three variables keep track of our download progress and are used to make sure
    // all 3 downloads finish.
    var loadingItems = false
    var loadingCollections = false
    var loadingTrash = false

    override fun refreshLibrary(useSmallLoadingAnimation: Boolean) {
        if (!state.usingGroup) {
            downloadLibrary(doRefresh = true, useSmallLoadingAnimation = useSmallLoadingAnimation)
        } else {
            this.loadGroup(state.currentGroup!!, refresh = true)
        }
    }

    fun shouldIUpdateLibrary(): Boolean {
        if (!zoteroDB.hasLegacyStorage()) {
            return true
        }
        val currentTimestamp = System.currentTimeMillis()
        val lastModified = zoteroDB.getLastModifiedTimestamp()

        if (TimeUnit.MILLISECONDS.toHours(currentTimestamp - lastModified) >= 24) {
            return true
        }
        return false
    }

    override fun isLoaded(): Boolean {
        return zoteroDB.isPopulated()
    }

    private fun filterItems(items: List<Item>): List<Item> {
        val onlyNotes = preferences.getIsShowingOnlyNotes()
        val onlyPdfs = preferences.getIsShowingOnlyPdfs()

        var newItems = items
        if (onlyNotes) {
            newItems =
                newItems.filter { zoteroDB.getNotes(it.itemKey).isNotEmpty() }
        }
        if (onlyPdfs) {
            newItems = newItems.filter {
                getAttachments(it.itemKey).fold(false, { acc, attachment ->
                    var result = acc
                    if (!result) {
                        result = attachment.data["contentType"] == "application/pdf"
                    }
                    result
                })
            }
        }
        return newItems
    }

    override fun getGroupByTitle(groupTitle: String): GroupInfo? {
        return this.groups?.filter { it.name == groupTitle }?.firstOrNull()
    }

    override fun getLibraryItems(): List<Item> {
        return filterItems(zoteroDB.getDisplayableItems())
    }

    override fun getItemsFromCollection(collectionKey: String): List<Item> {
        val items = zoteroDB.getItemsFromCollection(collectionKey)
        return filterItems(items)
    }

    override fun getSubCollections(collectionKey: String): List<Collection> {
//        val collectionKey = zoteroDB.getCollectionId(collectionName)
        return zoteroDB.getSubCollectionsFor(collectionKey)
    }

    override fun downloadLibrary(doRefresh: Boolean, useSmallLoadingAnimation: Boolean) {
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


        if (doRefresh == false && zoteroDB.isPopulated()) {
            Log.d("zotero", "The library is already loaded, not continuing")
            return
        }
        if (loadingCollections || loadingItems || loadingTrash) {
            Log.e("zotero", "Error, we are already loading our library! not doing it again.")
            return
        }


        // show our loading animation.
        if (useSmallLoadingAnimation) {
            presenter.showBasicSyncAnimation()
        } else {
            presenter.showLibraryLoadingAnimation()
        }

        val db = zoteroDB

        Log.d("zotero", "loading library for group ${db.groupID}")

        loadItems(db, useSmallLoadingAnimation)
        loadCollections(db)
        loadTrashedItems(db)
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
                        presenter.createErrorAlert(
                            "Invalid API Key",
                            "Your API Key is invalid. To rectify this issue please clear app data for the app. " +
                                    "Then relogin."
                        ) {}
                    } else {
                        val bundle = Bundle()
                        bundle.putString("error_message", e.message)
                        firebaseAnalytics.logEvent("error_download_collections", bundle)
                        presenter.createErrorAlert(
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
        val observable = zoteroAPI.getTrashedItems(db.groupID, db.getTrashVersion()).map{response ->
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
                zoteroDatabase.writeItemPOJOs(GroupInfo.NO_GROUP_ID, response.items).blockingAwait()
            }
            response // pass it on.
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : Observer<ZoteroAPIItemsResponse> {
                var libraryVersion = -1 //dummy value, will be replaced in onNext()
                var downloaded = progress?.nDownloaded ?: 0
                override fun onComplete() {
                    if (db.getLibraryVersion() > -1) {
                        if (downloaded == 0) {
                            presenter.makeToastAlert("Already up to date.")
                        } else {
                            presenter.makeToastAlert("Updated ${downloaded} items.")
                        }
                    }
                    db.destroyDownloadProgress()
                    db.setItemsVersion(libraryVersion)
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
                            presenter.updateLibraryRefreshProgress(
                                downloaded,
                                response.totalResults,
                                ""
                            )
                        }
                        Log.d("zotero", "got ${this.downloaded} of ${response.totalResults} items")
                    } else {
                        Log.d("zotero", "got back cached response for items.")
                    }
                }

                override fun onError(e: Throwable) {
                    if (e is UpToDateException) {
                        Log.d("zotero", "our items are already up to date.")
                    } else if (e is LibraryVersionMisMatchException) {
                        // we need to redownload items but without the progress.
                        Log.d("zotero", "mismatched, reloading items.")
                        db.destroyDownloadProgress()
                        presenter.makeToastAlert("Could not continue, library has changed since last sync.")
                        firebaseAnalytics.logEvent(
                            "required_library_resync_from_mismatch",
                            Bundle()
                        )
                        loadItems(db, useSmallLoadingAnimation = false)
                        return
                    } else {
                        val bundle = Bundle()
                        bundle.putString("error_message", e.message)
                        firebaseAnalytics.logEvent("error_download_items", bundle)
                        presenter.createErrorAlert("Error downloading items", "message: ${e}", {})
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

    fun loadLibraryLocally() {
        if (!zoteroDB.isPopulated())
            finishLoading(zoteroDB)
    }

    fun loadLibraryStage2(db: ZoteroDB) {
        // as  defined above, stage 2 handles deleted files.
        if (loadingTrash || loadingCollections || loadingItems) {
            throw Exception("Error cannot proceed to stage 2 if library still loading.")
        }
        // todo implement deleted items here.

        finishLoading(db)


    }

    private fun finishLoading(db: ZoteroDB) {
        Log.d("zotero", "finished library loading.")


        val loadCollections = db.loadCollectionsFromDatabase().doOnError { e ->
            Log.e("zotero", "loading collections from db got error $e")
            firebaseAnalytics.logEvent(
                "error_load_collections_from_db",
                Bundle().apply { putString("error_message", "$e") })
            db.collections = LinkedList()
        }

        val loadItems = zoteroDB.loadItemsFromDatabase().doOnError { e ->
            Log.e("zotero", "loading Items from db got error $e")
            firebaseAnalytics.logEvent(
                "error_load_items_from_db",
                Bundle().apply { putString("error_message", "$e") })
            db.items = LinkedList()
        }

        loadCollections
            .andThen(loadItems)
            .andThen(zoteroDB.loadTrashItemsFromDB())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                presenter.hideLibraryLoadingAnimation()
                presenter.receiveCollections(getCollections())
                if (state.currentCollection == "unset") {
                    presenter.setCollection("all")
                } else {
                    presenter.redisplayItems()
                }
                if (db.items?.size == 0) {
                    // incase there was an error, i don't want users to be stuck with an empty library.
                    db.setItemsVersion(0)
                }


                this.checkAllAttachmentsForModification()
            }.doOnError {
                firebaseAnalytics.logEvent(
                    "error_finish_loading",
                    Bundle().apply { putString("error_message", it.toString()) })
                presenter.createErrorAlert("error loading library", "got error message ${it}", {})
            }.subscribe()
    }

    override fun getCollections(): List<Collection> {
        return zoteroDB.collections ?: LinkedList()
    }

    override fun getAttachments(itemKey: String): List<Item> {
        return zoteroDB.getAttachments(itemKey)
    }

    override fun getNotes(itemKey: String): List<Note> {
        return zoteroDB.getNotes(itemKey)
    }

    override fun filterCollections(query: String): List<Collection> {
        val queryUpper = query.toUpperCase(Locale.ROOT)
        return zoteroDB.collections?.filter {
            it.name.toUpperCase(Locale.ROOT).contains(queryUpper)
        } ?: LinkedList()
    }

    override fun openPDF(attachment: Item) {
        /* This method creates an intent to open a PDF for Android. */

        Completable.fromAction {
            // used to check attachments for filechanges.
            if (preferences.isAttachmentUploadingEnabled()) {
                zoteroDatabase.addRecentlyOpenedAttachments(
                    RecentlyOpenedAttachment(
                        attachment.itemKey,
                        attachment.getVersion()
                    )
                )
                    .blockingAwait()
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnComplete {
            try {
                val intent = attachmentStorageManager.openAttachment(attachment)
                context.startActivity(intent)
            } catch (exception: ActivityNotFoundException) {
                presenter.createErrorAlert("No PDF Viewer installed",
                    "There is no app that handles ${attachment.getFileExtension()} documents available on your device. Would you like to install one?",
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=com.xodo.pdf.reader")
                                )
                            )
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=com.xodo.pdf.reader")
                                )
                            )
                        }
                    })
            } catch (e: IllegalArgumentException) {
                presenter.createErrorAlert("Error opening File",
                    "There was an error opening your PDF." +
                            " If you are on a huawei device, this is a known error with their implementation of file" +
                            " access. Try changing the storage location to a custom path in settings.",
                    {})
                firebaseAnalytics.logEvent("open_pdf_illegal_argument_exception", Bundle())
            } catch (e: FileNotFoundException) {
                presenter.createErrorAlert("File not found",
                    "There was an error opening your PDF." +
                            "Please redownload your file.",
                    {})
                firebaseAnalytics.logEvent("open_pdf_file_not_found", Bundle())
            }
        }.doOnError {
            if (it is FileNotFoundException) {
                Log.d("zotero", "file not found")
            } else {
                throw it
            }
        }.subscribe()
    }

    override fun openAttachment(item: Item) {
        /* This is the point of entry when a user clicks an attachment on the UI.
        *  We must decide whether we want to intitiate a download or just open a local copy. */

        // check to see if the attachment exists but is invalid
        val attachmentExists: Boolean
        try {
            attachmentExists = attachmentStorageManager.checkIfAttachmentExists(
                item,
                checkMd5 = false
            )
        } catch (e: Exception) {
            presenter.makeToastAlert("could not open attachment, file not found.")
            return
        }
        if (attachmentExists) {
            if (zoteroDB.hasMd5Key(
                    item,
                    onlyWebdav = preferences.isWebDAVEnabled()
                ) && !attachmentStorageManager.validateMd5ForItem(
                    item,
                    zoteroDB.getMd5Key(item)
                )
            ) {
                presenter.createYesNoPrompt(
                    "File conflict",
                    "Your local copy is different to the server's. Would you like to redownload the server's copy?",
                    "Yes", "No", {
                        presenter.updateAttachmentDownloadProgress(0, -1)
                        attachmentStorageManager.deleteAttachment(item)
                        downloadAttachment(item)
                    }, {
                        openPDF(item)
                    }
                )
                return
            } else {
                openPDF(item)
            }

        } else {
            presenter.updateAttachmentDownloadProgress(0, -1)
            downloadAttachment(item)
        }

    }

    override fun filterItems(query: String): List<Item> {
        val items = zoteroDB.items?.filter {
            it.query(query)

        } ?: LinkedList<Item>()

        return items
    }

    var downloadDisposable: Disposable? = null

    override fun downloadAttachment(item: Item) {
        if (isDownloading) {
            Log.d("zotero", "not downloading ${item.getTitle()} because i am already downloading.")
            return
        }
        isDownloading = true

        zoteroAPI.downloadItemRx(item, state.currentGroup?.id ?: GroupInfo.NO_GROUP_ID, context)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<DownloadProgress> {
                var receivedMetadata = false

                override fun onComplete() {
                    isDownloading = false
                    presenter.finishDownloadingAttachment()

                    if (zoteroDB.hasMd5Key(item) && !attachmentStorageManager.validateMd5ForItem(
                            item, zoteroDB.getMd5Key(item)
                        )
                    ) {
                        Log.d("zotero", "md5 error on attachment ${zoteroDB.getMd5Key(item)}")
                        presenter.createErrorAlert(
                            "MD5 Verification Error",
                            "The download process did not complete properly. Please retry",
                            {})
                        firebaseAnalytics.logEvent("error_downloading_file_corrupted", Bundle())
                        attachmentStorageManager.deleteAttachment(item)
                        return
                    } else {
                        openPDF(item)
                    }
                }

                override fun onSubscribe(d: Disposable) {
                    downloadDisposable = d

                }

                override fun onNext(t: DownloadProgress) {
                    presenter.updateAttachmentDownloadProgress(t.progress, t.total)
                    if (!receivedMetadata && t.metadataHash != "") {
                        receivedMetadata = true
                        zoteroDB.updateAttachmentMetadata(
                            item.itemKey,
                            t.metadataHash,
                            t.mtime,
                            if (preferences.isWebDAVEnabled()) {
                                AttachmentInfo.WEBDAV
                            } else {
                                AttachmentInfo.ZOTEROAPI
                            }
                        ).subscribeOn(Schedulers.io()).subscribe()
                    }
                }

                override fun onError(e: Throwable) {
                    Log.e("zotero", "got error ${e}")
//                    if (downloadDisposable?.isDisposed == true) {
//                        return
//                    }

                    if (e is ZoteroNotFoundException) {
                        presenter.attachmentDownloadError(
                            "The file does not exist on the Zotero server."
                        )
                    } else {

                        firebaseAnalytics.logEvent(
                            "error_downloading_attachment",
                            Bundle().apply { putString("message", "${e.message}") })
                        presenter.attachmentDownloadError(
                            "Error Message: ${e.message}"
                        )
                    }
                    isDownloading = false
                }

            })
    }


    override fun cancelAttachmentDownload() {
        Log.d("zotero", "cancelling download")
        this.isDownloading = false
        downloadDisposable?.dispose()
    }

    var isDownloading: Boolean = false
    var currentlyDownloadingAttachment: Item? = null

    override fun getUnfiledItems(): List<Item> {
        if (!zoteroDB.isPopulated()) {
            Log.e("zotero", "error zoteroDB not populated!")
            return LinkedList()
        }
        return filterItems(zoteroDB.getItemsWithoutCollection())
    }

    override fun createNote(note: Note) {
        firebaseAnalytics.logEvent("create_note", Bundle())
        if (state.usingGroup) {
            presenter.makeToastAlert("Sorry, this isn't supported in shared collections.")
            return
        }
        zoteroAPI.uploadNote(note)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onComplete() {
                    presenter.hideBasicSyncAnimation()
                    loadItems(db = zoteroDB, useSmallLoadingAnimation = true)
                    presenter.makeToastAlert("Successfully created your note")
                }

                override fun onSubscribe(d: Disposable) {
                    presenter.showBasicSyncAnimation()
                }

                override fun onError(e: Throwable) {
                    firebaseAnalytics.logEvent(
                        "create_note_error",
                        Bundle().apply { putString("error_message", e.toString()) })
                    presenter.createErrorAlert(
                        "Error creating note",
                        "An error occurred while trying to create your note. Message: ${e}",
                        {})
                }
            })


    }

    override fun modifyNote(note: Note) {
        firebaseAnalytics.logEvent("modify_note", Bundle())
        if (state.usingGroup) {
            presenter.makeToastAlert("Sorry, this isn't supported in shared collections.")
            return
        }
        zoteroAPI.modifyNote(note, zoteroDB.getLibraryVersion())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onComplete() {
                    presenter.hideBasicSyncAnimation()
                    loadItems(db = zoteroDB, useSmallLoadingAnimation = true)
                    presenter.makeToastAlert("Successfully modified your note")
                }

                override fun onSubscribe(d: Disposable) {
                    presenter.showBasicSyncAnimation()
                }

                override fun onError(e: Throwable) {
                    firebaseAnalytics.logEvent(
                        "modify_note_error",
                        Bundle().apply { putString("error_message", e.toString()) })
                    if (e is ItemLockedException) {
                        presenter.createErrorAlert(
                            "Error modifying note",
                            "The note you are editing has been locked or you do not have permission to change it.",
                            {})
                    } else if (e is ItemChangedSinceException) {
                        presenter.createErrorAlert(
                            "Error modifying note",
                            "The version on Zotero's servers is newer than your local copy. Please refresh your library.",
                            {})

                    } else {
                        presenter.createErrorAlert(
                            "Error modifying note",
                            "An error occurred while trying to create your note. Message: ${e}",
                            {})
                    }
                    presenter.hideBasicSyncAnimation()
                }
            })
    }

    override fun deleteNote(note: Note) {
        firebaseAnalytics.logEvent("delete_note", Bundle())
        if (state.usingGroup) {
            presenter.makeToastAlert("Sorry, this isn't supported in shared collections.")
            return
        }
        zoteroAPI.deleteItem(note.key, note.version, object : DeleteItemListener {
            override fun success() {
                presenter.makeToastAlert("Successfully deleted your note.")
                zoteroDB.deleteItem(note.key)
                presenter.refreshItemView()
            }

            override fun failedItemLocked() {
                presenter.createErrorAlert("Error Deleting Note", "The item is locked " +
                        "and you do not have permission to delete this note.", {})
            }

            override fun failedItemChangedSince() {
                presenter.createErrorAlert("Error Deleting Note",
                    "Your local copy of this note is out of date. " +
                            "Please refresh your library to delete this note.",
                    {})
            }

            override fun failed(code: Int) {
                presenter.createErrorAlert("Error Deleting Note", "There was an error " +
                        "deleting your note. The server responded : ${code}", {})
            }
        })
    }

    override fun deleteAttachment(item: Item) {
        zoteroAPI.deleteItem(item.itemKey, item.getVersion(), object : DeleteItemListener {
            override fun success() {
                presenter.makeToastAlert("Successfully deleted your attachment.")
                zoteroDB.deleteItem(item.itemKey)
                presenter.refreshItemView()
            }

            override fun failedItemLocked() {
                presenter.createErrorAlert("Error Deleting Attachment", "The item is locked " +
                        "and you do not have permission to delete this attachment.", {})
            }

            override fun failedItemChangedSince() {
                presenter.createErrorAlert("Error Deleting Attachment",
                    "Your local copy of this note is out of date. " +
                            "Please refresh your library to delete this attachment.",
                    {})
            }

            override fun failed(code: Int) {
                presenter.createErrorAlert("Error Deleting Attachment", "There was an error " +
                        "deleting your attachment. The server responded : ${code}", {})
            }
        })
    }

    fun displayGroupsOnUI() {
        val groups = zoteroDatabase.getGroups()
        groups.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : MaybeObserver<List<GroupInfo>> {
                override fun onSuccess(groupInfo: List<GroupInfo>) {
                    Log.d("zotero", "completed get group info.")
                    this@LibraryActivityModel.groups = groupInfo
                    presenter.displayGroupsOnActionBar(groupInfo)
                }

                override fun onComplete() {
                    Log.d("zotero", "User has no groups.")
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {
                    Log.e("zotero", "error loading groups.")
                    presenter.createErrorAlert(
                        "Error loading group data",
                        "Message: ${e.message}",
                        {})
                    val bundle = Bundle()
                    bundle.putString("error_message", e.message)
                    firebaseAnalytics.logEvent("error_loading_group_data", bundle)
                }

            })
    }

    fun loadGroups() {
        /* This method loads a list of groups. It does not deal with getting items or catalogs. */

        val groupsObserver = zoteroAPI.getGroupInfo()
        groupsObserver.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : Observer<List<GroupInfo>> {
                override fun onComplete() {
                    Log.d("zotero", "completed getting group info")
                    displayGroupsOnUI()
                }

                override fun onSubscribe(d: Disposable) {
                    Log.d("zotero", "subscribed to group info")
                }

                override fun onNext(groupInfo: List<GroupInfo>) {
                    groupInfo.forEach {
                        val status = zoteroDatabase.addGroup(it)
                        status.blockingAwait() // wait until the db add is finished.
                    }
                }

                override fun onError(e: Throwable) {
                }

            })
    }

    fun checkAllAttachmentsForModification() {
        if (isUsingGroups()) {
            Log.d(
                "zotero",
                "not checking attachments because we do not support groups"
            )
            return
        }

        if (!preferences.isAttachmentUploadingEnabled()) {
            Log.d("zotero", "Not checking attachments, disabled by preferences")
            return
        }

        zoteroDatabase.getRecentlyOpenedAttachments()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).map { listOfRecently ->
                val itemsToUpload: MutableList<Pair<Item, Int>> = LinkedList()
                for (recentlyOpenedAttachment in listOfRecently) {
                    Log.d("zotero", "RECENTLY OPENED ${recentlyOpenedAttachment.itemKey}")
                    val item = zoteroDB.getItemWithKey(recentlyOpenedAttachment.itemKey)
                    if (item != null) {
                        try {
                            val md5Key = zoteroDB.getMd5Key(item)
                            if (md5Key != "" && attachmentStorageManager.validateMd5ForItem(
                                    item,
                                    md5Key
                                ) == false
                            ) {
                                // our attachment has been modified, we will offer to upload.
                                itemsToUpload.add(Pair(item, recentlyOpenedAttachment.version))
                                Log.d(
                                    "zotero",
                                    "found change in ${item.getTitle()} ${item.itemKey}"
                                )
                            } else {
                                // the item hasnt changed.
                                removeFromRecentlyViewed(item)
                            }
                        } catch (e: FileNotFoundException) {
                            Log.d(
                                "zotero",
                                "could not find local attachment with itemKey ${item.itemKey}"
                            )
                            removeFromRecentlyViewed(item)
                        } catch (e: Exception) {
                            Log.e("zotero", "validateMd5 got error $e")
                            val bundle = Bundle().apply {
                                putString("error_message", e.toString())
                            }
                            firebaseAnalytics.logEvent("error_check_attachments", bundle)
                            removeFromRecentlyViewed(item)
                        }
                    }
                }
                itemsToUpload
            }.subscribe(object : MaybeObserver<MutableList<Pair<Item, Int>>> {
                override fun onComplete() {
                    // do nothing
                }

                override fun onSubscribe(d: Disposable) {
                    // do nothing
                }


                override fun onError(e: Throwable) {
                    Log.e("zotero", "validateMd5 observer got error $e")
                    val bundle = Bundle().apply {
                        putString("error_message", e.message)
                    }
                    firebaseAnalytics.logEvent("error_check_attachments", bundle)
                }

                override fun onSuccess(itemsToUpload: MutableList<Pair<Item, Int>>) {
                    if (itemsToUpload.isNotEmpty()) {
                        askToUploadAttachments(itemsToUpload)
                    }
                }

            })
    }

    fun askToUploadAttachments(changedAttachments: List<Pair<Item, Int>>) {
        // for the sake of sanity I will only ask to upload 1 attachment.
        // this is because of limitations of only having 1 upload occur concurrently
        // and my unwillingness to implement a chaining mechanism for uploads for what i expect to
        // be a niche power user.
        val attachment = changedAttachments.first().first
        val version = changedAttachments.first().second
        val fileSizeBytes = attachmentStorageManager.getFileSize(
            attachmentStorageManager.getAttachmentUri(attachment)
        )

        if (fileSizeBytes == 0L) {
            Log.e("zotero", "avoiding uploading a garbage PDF")
            FirebaseAnalytics.getInstance(context)
                .logEvent("AVOIDED_UPLOAD_GARBAGE", Bundle())
            attachmentStorageManager.deleteAttachment(attachment)
            removeFromRecentlyViewed(attachment)
            return
        }

        val sizeKiloBytes = "${fileSizeBytes / 1000}KB"

        val message =
            "${attachment.data["filename"]!!} ($sizeKiloBytes) is different to Zotero's version. Would you like to upload this PDF to replace the remote version?"

        presenter.createYesNoPrompt(
            "Detected changes to attachment",
            message,
            "Upload",
            "No",
            {
                if (version < attachment.getVersion()) {
                    firebaseAnalytics.logEvent("upload_attachment_version_mismatch", Bundle())
                    presenter.createYesNoPrompt("Outdated Version",
                        "This local copy is older than the version on Zotero's server, are you sure you upload (this will irreversibly overwrite the server's copy)",
                        "I am sure",
                        "Cancel",
                        { uploadAttachment(attachment) },
                        { removeFromRecentlyViewed(attachment) })
                }
                uploadAttachment(attachment)
            },
            { removeFromRecentlyViewed(attachment) })
    }

    override fun uploadAttachment(attachment: Item) {
        val md5Key = attachmentStorageManager.calculateMd5(attachment)
        var mtime = attachmentStorageManager.getMtime(attachment)

        if (mtime < attachment.getMtime()) {
            Log.e("zotero", "for some reason our mtime is older than the original???")
            mtime = attachment.getMtime()
        }
        if (preferences.isWebDAVEnabled()) {
            zoteroAPI.uploadAttachmentWithWebdav(attachment, context).andThen(
                Completable.fromAction {
                    // TODO extract this complexity to some other class.
                    if (preferences.isWebDAVEnabled()) {
                        val modificationJsonObject = JsonObject()
                        modificationJsonObject.addProperty("md5", md5Key)
                        modificationJsonObject.addProperty("mtime", mtime)
                        zoteroAPI.patchItem(attachment, modificationJsonObject).blockingAwait()
                    }
                }
            )
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                    object : CompletableObserver {
                        override fun onComplete() {
                            presenter.stopUploadingAttachment()
                            removeFromRecentlyViewed(attachment)
                            zoteroDB.updateAttachmentMetadata(
                                attachment.itemKey,
                                attachmentStorageManager.calculateMd5(attachment),
                                attachmentStorageManager.getMtime(attachment),
                                AttachmentInfo.WEBDAV
                            ).subscribeOn(Schedulers.io()).subscribe()
                            firebaseAnalytics.logEvent(
                                "upload_attachment_successful_webdav",
                                Bundle()
                            )
                        }

                        override fun onSubscribe(d: Disposable) {
                            presenter.startUploadingAttachment(attachment)
                        }

                        override fun onError(e: Throwable) {
                            presenter.createErrorAlert(
                                "Error uploading Attachment",
                                e.toString(),
                                {})
                            Log.e("zotero", "got exception: $e")
                            val bundle =
                                Bundle().apply { putString("error_message", e.toString()) }
                            firebaseAnalytics.logEvent(
                                "error_uploading_attachments_webdav",
                                bundle
                            )
                            presenter.stopUploadingAttachment()
                        }
                    })
            return
        } else {
            zoteroAPI.updateAttachment(attachment).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(object :
                    CompletableObserver {
                    override fun onComplete() {
                        presenter.stopUploadingAttachment()
                        removeFromRecentlyViewed(attachment)
                        zoteroDB.updateAttachmentMetadata(
                            attachment.itemKey,
                            attachmentStorageManager.calculateMd5(attachment),
                            attachmentStorageManager.getMtime(attachment),
                            AttachmentInfo.ZOTEROAPI
                        ).subscribeOn(Schedulers.io()).subscribe()
                    }

                    override fun onSubscribe(d: Disposable) {
                        presenter.startUploadingAttachment(attachment)
                    }

                    override fun onError(e: Throwable) {
                        if (e is com.mickstarify.zooforzotero.ZoteroAPI.AlreadyUploadedException) {
                            removeFromRecentlyViewed(attachment)
                            zoteroDB.updateAttachmentMetadata(
                                attachment.itemKey,
                                attachmentStorageManager.calculateMd5(attachment),
                                attachmentStorageManager.getMtime(attachment),
                                AttachmentInfo.WEBDAV
                            ).subscribeOn(Schedulers.io()).subscribe()
                        } else if (e is PreconditionFailedException) {
                            presenter.createErrorAlert(
                                "Error uploading Attachment",
                                "The server's copy of this attachment is newer than yours. " +
                                        "Please sync your library again to get the up to date version. " +
                                        "You will need to back up your annotated file first if you wish to keep this changes.",
                                {})
                        } else if (e is RequestEntityTooLarge) {
                            presenter.createErrorAlert(
                                "Error uploading Attachment",
                                "You do not have enough storage quota to store this atttachment.",
                                {})
                        } else {
                            presenter.createErrorAlert(
                                "Error uploading Attachment",
                                e.toString(),
                                {})
                            Log.e("zotero", "got exception: $e")
                            val bundle =
                                Bundle().apply { putString("error_message", e.toString()) }
                            firebaseAnalytics.logEvent("error_uploading_attachments", bundle)
                        }
                        presenter.stopUploadingAttachment()
                    }

                })
        }
    }

    override fun removeFromRecentlyViewed(attachment: Item) {
        zoteroDatabase.deleteRecentlyOpenedAttachment(attachment.itemKey)
            .subscribeOn(Schedulers.io()).subscribe()
    }

    fun loadGroupItemsLocally(group: GroupInfo, db: ZoteroDB) {
        db.loadItemsFromDatabase().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).doOnComplete {
                finishLoadingGroups(group)
            }.subscribe()
    }

    override fun loadGroup(
        group: GroupInfo,
        refresh: Boolean
    ) {
        /* This gets the items/catalogs for a group and does the respective callbacks to display the group.*/

        if (loadingItems || loadingCollections) {
            Log.e("zotero", "already loading group data!")
            return
        }

        loadingItems = true
        loadingCollections = true

        /* This method beings the network calls to download all items related to the user group. */
        val db = zoteroGroupDB.getGroup(group.id)
        if (refresh == false && db.isPopulated()) {
            // user has already loaded this group so we just need to display it.
            finishLoadingGroups(group)
            return
        }

        val modifiedSince = db.getLibraryVersion()

        // todo add progress later.
        val groupItemObservable =
            zoteroAPI.getItems(
                modifiedSince,
                groupID = group.id,
                downloadProgress = null
            )
        groupItemObservable.map { response ->
            // check to see if there are any items.
            if (response.isCached == false) {
                zoteroDatabase.writeItemPOJOs(group.id, response.items).blockingAwait()
            }
            response // pass it on.
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : Observer<ZoteroAPIItemsResponse> {
                var libraryVersion = -1
                var downloaded = 0
                override fun onComplete() {
                    if (db.getLibraryVersion() > -1) {
                        if (downloaded == 0) {
                            presenter.makeToastAlert("Already up to date.")
                        } else {
                            presenter.makeToastAlert("Updated ${downloaded} items.")
                        }
                    }
                    db.setItemsVersion(libraryVersion)
                    loadGroupItemsLocally(group, db)
                    finishLoadingGroups(group)
                }

                override fun onSubscribe(d: Disposable) {
                    presenter.showLibraryLoadingAnimation()
                }

                override fun onNext(response: ZoteroAPIItemsResponse) {
                    if (response.isCached == false) {
                        this.libraryVersion = response.LastModifiedVersion
                        this.downloaded += response.items.size

                        presenter.updateLibraryRefreshProgress(
                            downloaded,
                            response.totalResults,
                            ""
                        )
                        Log.d(
                            "zotero",
                            "got ${this.downloaded} of ${response.totalResults} items"
                        )
                    } else {
                        Log.d("zotero", "got back cached response for items.")
                    }
                }

                override fun onError(e: Throwable) {
                    if (e is UpToDateException) {
                        loadGroupItemsLocally(group, db)
                    } else {
                        val bundle = Bundle()
                        bundle.putString("error_message", e.message)
                        firebaseAnalytics.logEvent("error_download_group_items", bundle)
                        presenter.createErrorAlert(
                            "Error downloading items",
                            "message: ${e}",
                            {})
                        Log.e("zotero", "${e}")
                        Log.e("zotero", "${e.stackTrace}")
                        loadGroupItemsLocally(group, db)
                    }
                }
            })

        val groupCollectionsObservable =
            zoteroAPI.getCollections(
                modifiedSince,
                groupID = group.id
            )
        groupCollectionsObservable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : Observer<List<CollectionPOJO>> {
                val collections = LinkedList<CollectionPOJO>()
                override fun onComplete() {
                    val collectionObjects = collections.map { Collection(it, group.id) }
                    db.collections = collectionObjects
                    db.commitCollectionsToDatabase()
                    if (db.items != null) {
                        finishLoadingGroups(group)
                    }
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(collections: List<CollectionPOJO>) {
                    this.collections.addAll(collections)
                }

                override fun onError(e: Throwable) {
                    if (e is UpToDateException) {
                        db.loadCollectionsFromDatabase().subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete {
                                if (db.items != null) {
                                    finishLoadingGroups(group)
                                }
                            }.subscribe()

                    } else if (e is APIKeyRevokedException) {
                        presenter.createErrorAlert(
                            "Permission Error",
                            "403: Unauthorized access attempt. Please verify your api key hasn't been revoked and you" +
                                    "still have readable access to the group."
                        ) {}
                    } else {
                        presenter.createErrorAlert(
                            "Error downloading Group Collections",
                            "Message: ${e}"
                        ) {}

                        db.loadCollectionsFromDatabase().subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete {
                                if (db.items != null) {
                                    finishLoadingGroups(group)
                                }
                            }.subscribe()
                    }
                }
            })
    }

    private fun finishLoadingGroups(group: GroupInfo) {
        val db = zoteroGroupDB.getGroup(group.id)
        if (db.items == null || db.collections == null) {
            Log.d("zotero", "not finished loading groups yet")
            return
        }
        Log.d("zotero", "Finished loading group with id ${group.id}")
        loadingItems = false
        loadingCollections = false
        presenter.hideLibraryLoadingAnimation()
        state.usingGroup = true
        state.currentGroup = group
        zoteroDBPicker.groupId = group.id
        state.currentCollection = "group_all"
        presenter.redisplayItems()
    }

    override fun usePersonalLibrary() {
        state.usingGroup = false
        state.currentGroup = null
        this.zoteroDBPicker.stopGroup()
    }

    fun getCurrentCollection(): String {
        return state.currentCollection
    }

    fun setCurrentCollection(collectionName: String) {
        state.currentCollection = collectionName
    }

    fun isUsingGroups(): Boolean {
        return state.usingGroup
    }

    fun getCurrentGroup(): GroupInfo {
        return state.currentGroup ?: throw Exception("Error there is no current Group.")
    }

    fun checkAttachmentStorageAccess() {
        try {
            if (attachmentStorageManager.testStorage() == false) {
                throw Exception()
            }
        } catch (e: Exception) {
            Log.e("zotero", "error testing storage. ${e}")
            presenter.createErrorAlert(
                "Permission Error",
                "There was an error accessing your zotero attachment location. Please reconfigure in settings.",
                {})
            preferences.useExternalCache()
        }
    }

    fun hasOldStorage(): Boolean {
        return zoteroDB.hasLegacyStorage()
    }

    //TODO i will delete this code next version. (just for version 2.2)
    fun migrateFromOldStorage() {
        zoteroDB.migrateItemsFromOldStorage().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe(object : CompletableObserver {
                override fun onComplete() {
                    zoteroDB.scanAndIndexAttachments(attachmentStorageManager)
                        .doOnError({ e ->
                            Log.e("zotero", "migration error ${e}")
                            firebaseAnalytics.logEvent(
                                "migration_error_file_index",
                                Bundle().apply { putString("error_message", "$e") })
                            downloadLibrary()
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(Consumer {
                            downloadLibrary()
                        })
                }

                override fun onSubscribe(d: Disposable) {
                    Log.d("zotero", "Migrating storage from old database")
                }

                override fun onError(e: Throwable) {
                    Log.e("zotero", "migration error ${e}")
                    firebaseAnalytics.logEvent(
                        "migration_error",
                        Bundle().apply { putString("error_message", "$e") })
                    presenter.createErrorAlert(
                        "Error migrating data",
                        "Sorry. There was an error migrating your items. You will need to do a full resync.",
                        {}
                    )
                    zoteroDB.deleteLegacyStorage()
                    zoteroDB.setItemsVersion(0)
                    downloadLibrary()
                }
            })
    }

    fun updateDeletedEntries(): Single<Completable> {
        /* Checks for deleted entries on the zotero servers and mirrors those changes on the local database. */
        // we have to assume the library is loaded.

        val deletedItemsCheckVersion = zoteroDB.getLastDeletedItemsCheckVersion()
        val libraryVersion = zoteroDB.getLibraryVersion()
        if (deletedItemsCheckVersion == libraryVersion) {
            Log.d(
                "zotero",
                "not checking deleted items because library hasn't changed. ${libraryVersion}"
            )
            return Single.just(Completable.complete()) // our job is done, there is nothing to check.
        }

        val single = zoteroAPI.getDeletedEntries(deletedItemsCheckVersion, zoteroDB.groupID)
            .map {
                val completable = Completable.complete()
                if (it.items.size > 0) {
                    completable.andThen(zoteroDB.deleteItems(it.items))
                }
                if (it.collections.size > 0) {
                    completable.andThen(zoteroDB.deleteCollections(it.collections))
                }
                completable.doOnComplete {
                    Log.d(
                        "zotero",
                        "Setting deletedLibraryVersion to $libraryVersion from $deletedItemsCheckVersion"
                    )
                    zoteroDB.setLastDeletedItemsCheckVersion(libraryVersion)
                }
            }.subscribeOn(Schedulers.io())
        return single
    }

    init {
        ((context as Activity).application as ZooForZoteroApplication).component.inject(this)
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val auth = AuthenticationStorage(context)

        if (auth.hasCredentials()) {
            zoteroAPI = ZoteroAPI(
                auth.getUserKey(),
                auth.getUserID(),
                auth.getUsername(),
                attachmentStorageManager
            )
        } else {
            presenter.createErrorAlert(
                "Error with stored API",
                "The API Key we have stored in the application is invalid!" +
                        "Please re-authenticate the application"
            ) { context.finish() }
            auth.destroyCredentials()
            zoteroDB.clearItemsVersion()
        }

        checkAttachmentStorageAccess()

        /*Do to a change in implementation this code will transition webdav configs.*/
        if (preferences.firstRunForVersion25()) {
            if (preferences.isWebDAVEnabled()) {
                val address = preferences.getWebDAVAddress()
                val newAddress = if (address.endsWith("/zotero")) {
                    address
                } else {
                    if (address.endsWith("/")) { // so we don't get server.com//zotero
                        address + "zotero"
                    } else {
                        address + "/zotero"
                    }
                }
                preferences.setWebDAVAuthentication(
                    newAddress,
                    preferences.getWebDAVUsername(),
                    preferences.getWebDAVPassword()
                )
            }
        }
    }


    fun getTrashedItems(): List<Item> {
        return zoteroDB.getTrashItems()
    }

    fun getCollectionFromKey(collectionKey: String): Collection? {
        return zoteroDB.getCollectionById(collectionKey)
    }
}