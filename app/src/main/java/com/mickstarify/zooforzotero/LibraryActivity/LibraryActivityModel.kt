package com.mickstarify.zooforzotero.LibraryActivity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import com.mickstarify.zooforzotero.ZoteroAPI.*
import com.mickstarify.zooforzotero.ZoteroAPI.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroAPI.Database.ZoteroDatabase
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import io.reactivex.MaybeObserver
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import java.io.File
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class LibraryActivityModel(private val presenter: Contract.Presenter, val context: Context) :
    Contract.Model {

    private var itemsDownloadAttempt = 0
    private var collectionsDownloadAttempt = 0

    // just a flag to store whether we have shown the user a network error so that we don't
    // do it twice (from getCatalog & getItems
    var shownNetworkError: Boolean = false

    // stores the current item being viewed by the user. (useful for refreshing the view)
    var selectedItem: Item? = null
    var isDisplayingItems = false

    var loadingItems = false
    var loadingCollections = false

    private var firebaseAnalytics: FirebaseAnalytics

    private lateinit var zoteroAPI: ZoteroAPI
    private val zoteroGroupDB = ZoteroGroupDB(context)
    private var zoteroDBPicker = ZoteroDBPicker(ZoteroDB(context), zoteroGroupDB)
    private val zoteroDatabase = ZoteroDatabase(context)
    private var groups: List<GroupInfo>? = null

    val preferences = PreferenceManager(context)

    val state = LibraryModelState()

    override fun refreshLibrary() {
        if (!state.usingGroup) {
            this.requestItems(useCaching = true)
            this.requestCollections(useCaching = true)
        } else {
            this.loadGroup(state.currentGroup!!, refresh = true)
        }
    }

    fun shouldIUpdateLibrary(): Boolean {
        if (!zoteroDBPicker.getZoteroDB().hasStorage()) {
            return true
        }
        val currentTimestamp = System.currentTimeMillis()
        val lastModified = zoteroDBPicker.getZoteroDB().getLastModifiedTimestamp()

        if (TimeUnit.MILLISECONDS.toHours(currentTimestamp - lastModified) >= 24) {
            return true
        }
        return false
    }

    override fun isLoaded(): Boolean {
        return zoteroDBPicker.getZoteroDB().isPopulated()
    }

    private fun filterItems(items: List<Item>): List<Item> {
        val onlyNotes = preferences.getIsShowingOnlyNotes()
        val onlyPdfs = preferences.getIsShowingOnlyPdfs()

        var newItems = items
        if (onlyNotes) {
            newItems =
                newItems.filter { zoteroDBPicker.getZoteroDB().getNotes(it.ItemKey).isNotEmpty() }
        }
        if (onlyPdfs) {
            newItems = newItems.filter {
                getAttachments(it.ItemKey).fold(false, { acc, attachment ->
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
        return filterItems(zoteroDBPicker.getZoteroDB().getDisplayableItems())
    }

    override fun getItemsFromCollection(collectionName: String): List<Item> {
        val collectionKey = zoteroDBPicker.getZoteroDB().getCollectionId(collectionName)
        if (collectionKey != null) {
            val items = zoteroDBPicker.getZoteroDB().getItemsFromCollection(collectionKey)
            return filterItems(items)
        }
        presenter.makeToastAlert("Error, could not open collection: $collectionName Try Refreshing your Library.")
        val bundle = Bundle()
        bundle.putBoolean("collections_is_null", zoteroDBPicker.getZoteroDB().collections == null)
        bundle.putBoolean("is_populated", zoteroDBPicker.getZoteroDB().isPopulated())
        bundle.putString("collection_name", collectionName)
        firebaseAnalytics.logEvent("error_getting_subcollections_get_items", bundle)
        return LinkedList()
    }

    override fun getSubCollections(collectionName: String): List<Collection> {
        val collectionKey = zoteroDBPicker.getZoteroDB().getCollectionId(collectionName)
        if (collectionKey != null) {
            return zoteroDBPicker.getZoteroDB().getSubCollectionsFor(collectionKey)
        }
        presenter.makeToastAlert("Error, could not open collection: $collectionName Try Refreshing your Library.")
        val bundle = Bundle()
        bundle.putBoolean("collection_is_null", zoteroDBPicker.getZoteroDB().collections == null)
        bundle.putBoolean("is_populated", zoteroDBPicker.getZoteroDB().isPopulated())
        bundle.putString("collection_name", collectionName)
        firebaseAnalytics.logEvent("error_getting_subcollections_get_sub", bundle)
        return LinkedList()
    }


    fun loadItemsLocally() {
        doAsync {
            zoteroDBPicker.getZoteroDB().loadItemsFromStorage()
            onComplete {
                finishGetItems()
            }
        }
    }

    override fun requestItems(useCaching: Boolean) {
        loadingItems = true
        itemsDownloadAttempt++

        val db = zoteroDBPicker.getZoteroDB()

        val groupItemObservable =
            zoteroAPI.getItems(db.getLibraryVersion(), useCaching, isGroup = false)
        groupItemObservable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : Observer<ZoteroAPIItemsResponse> {
                val items = LinkedList<Item>()
                var libraryVersion = -1
                override fun onComplete() {
                    if (db.getLibraryVersion() == -1) {
                        // these are all brand new items
                        db.items = items
                        db.commitItemsToStorage()
                    } else {
                        //otherwise we just have changes and need to apply them.
                        db.loadItemsFromStorage()
                        db.applyChangesToItems(items)
                        presenter.makeToastAlert("Updated ${items.size} items.")
                    }
                    db.setItemsVersion(libraryVersion)
                    finishGetItems()
                }

                override fun onSubscribe(d: Disposable) {
                    presenter.showLibraryLoadingAnimation()
                }

                override fun onNext(response: ZoteroAPIItemsResponse) {
                    assert(response.isCached == false)
                    this.libraryVersion = response.LastModifiedVersion
                    Log.d("zotero", "lv=${response.LastModifiedVersion}")
                    this.items.addAll(response.items)
                    presenter.updateLibraryRefreshProgress(this.items.size, response.totalResults)
                    Log.d("zotero", "got ${items.size} of ${response.totalResults} items")
                }

                override fun onError(e: Throwable) {
                    if (e is UpToDateException) {
                        db.loadItemsFromStorage()
                        finishGetItems()
                    } else {
                        presenter.createErrorAlert("Error downloading items", "message: ${e}", {})
                        Log.e("zotero", "${e}")
                        Log.e("zotero", "${e.stackTrace}")
                    }
                }
            })
    }

    override fun loadCollectionsLocally() {
        doAsync {
            zoteroDBPicker.getZoteroDB().loadCollectionsFromStorage()
            onComplete {
                finishGetCollections()
            }
        }
    }

    override fun requestCollections(useCaching: Boolean) {
        loadingCollections = true
        collectionsDownloadAttempt++

        val db = zoteroDBPicker.getZoteroDB()

        val collectionsObservable =
            zoteroAPI.getCollections(
                zoteroDBPicker.getZoteroDB().getLibraryVersion(),
                useCaching,
                isGroup = false
            )
        collectionsObservable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : Observer<List<Collection>> {
                val collections = LinkedList<Collection>()
                override fun onComplete() {
                    db.collections = collections
                    db.commitCollectionsToStorage()
                    finishGetCollections()
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(collections: List<Collection>) {
                    this.collections.addAll(collections)
                    Log.d("zotero", "got ${this.collections.size}")
                }

                override fun onError(e: Throwable) {
                    if (e is UpToDateException) {
                        db.loadCollectionsFromStorage()
                        finishGetCollections()
                    } else if (e is APIKeyRevokedException) {
                        presenter.createErrorAlert(
                            "Invalid API Key",
                            "Your API Key is invalid. To rectify this issue please clear app data for the app. " +
                                    "Then relogin."
                        ) {}
                    } else {
                        presenter.createErrorAlert(
                            "Error downloading Group Collections",
                            "Message: ${e}"
                        ) {}

                        if (db.hasStorage()) {
                            Log.d("zotero", "no internet connection. Using cached copy")
                            db.loadCollectionsFromStorage()
                            finishGetCollections()
                        }
                    }
                }
            })
    }

    private fun finishGetCollections() {
        if (zoteroDBPicker.getZoteroDB().collections != null) {
            loadingCollections = false
            if (zoteroDBPicker.getZoteroDB().items != null) {
                finishLoading()
            }
        }
    }

    private fun finishGetItems() {
        if (zoteroDBPicker.getZoteroDB().items != null) {
            loadingItems = false
            if (zoteroDBPicker.getZoteroDB().collections != null) {
                finishLoading()
            }
        }
    }

    private fun finishLoading() {
        presenter.hideLibraryLoadingAnimation()
        presenter.receiveCollections(getCollections())
        if (state.currentCollection == "unset") {
            presenter.setCollection("all")
        } else {
            presenter.redisplayItems()
        }
    }

    override fun getCollections(): List<Collection> {
        return zoteroDBPicker.getZoteroDB().collections ?: LinkedList()
    }

    override fun getAttachments(itemKey: String): List<Item> {
        return zoteroDBPicker.getZoteroDB().getAttachments(itemKey)
    }

    override fun getNotes(itemKey: String): List<Note> {
        return zoteroDBPicker.getZoteroDB().getNotes(itemKey)
    }

    override fun filterCollections(query: String): List<Collection> {
        val queryUpper = query.toUpperCase(Locale.ROOT)
        return zoteroDBPicker.getZoteroDB().collections?.filter {
            it.getName().toUpperCase(Locale.ROOT).contains(queryUpper)
        } ?: LinkedList()
    }

    override fun openPDF(attachment: File) {
        var intent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri: Uri?
            try {
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    attachment
                )
            } catch (exception: IllegalArgumentException) {
                val params = Bundle()
                params.putString("filename", attachment.name)
                firebaseAnalytics.logEvent("error_opening_pdf", params)
                presenter.makeToastAlert("Sorry an error occurred while trying to download your attachment.")
                return
            }
            Log.d("zotero", "${uri.query}")
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        } else {
            intent.setDataAndType(Uri.fromFile(attachment), "application/pdf")
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            intent = Intent.createChooser(intent, "Open File")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        }
        context.startActivity(intent)
    }

    override fun filterItems(query: String): List<Item> {
        val items = zoteroDBPicker.getZoteroDB().items?.filter {
            it.query(query)

        } ?: LinkedList()

        return items
    }

    var isDownloading: Boolean = false
    var task: Future<Unit>? = null
    override fun downloadAttachment(item: Item) {
        if (isDownloading) {
            Log.d("zotero", "not downloading ${item.getTitle()} because i am already downloading.")
            return
        }
        isDownloading = true

        zoteroAPI.downloadItem(context,
            item,
            state.usingGroup,
            groupID = (state.currentGroup?.id ?: 0),
            listener = object : ZoteroAPIDownloadAttachmentListener {
                override fun receiveTask(task: Future<Unit>) {
                    this@LibraryActivityModel.task = task
                    // we have to check if the user has stopped the download before this task has come back.
                    if (isDownloading == false) {
                        cancelAttachmentDownload()
                    }
                }

                override fun onNetworkFailure() {
                    presenter.attachmentDownloadError()
                    isDownloading = false
                }

                override fun onComplete(attachment: File) {
                    isDownloading = false
                    if (attachment.exists()) {
                        presenter.openPDF(attachment)
                    } else {
                        presenter.attachmentDownloadError()
                    }
                }

                override fun onFailure(message: String) {
                    presenter.attachmentDownloadError(message)
                    isDownloading = false
                }

                override fun onProgressUpdate(progress: Long, total: Long) {
                    if (task?.isCancelled != true) {
                        Log.d("zotero", "Downloading attachment. got $progress of $total")
                        presenter.updateAttachmentDownloadProgress(progress, total)
                    }
                }
            })
    }

    override fun getUnfiledItems(): List<Item> {
        if (!zoteroDBPicker.getZoteroDB().isPopulated()) {
            Log.e("zotero", "error zoteroDB not populated!")
            return LinkedList()
        }
        return filterItems(zoteroDBPicker.getZoteroDB().getItemsWithoutCollection())
    }

    override fun cancelAttachmentDownload() {
        this.isDownloading = false
        task?.cancel(true)
    }

    override fun createNote(note: Note) {
        firebaseAnalytics.logEvent("create_note", Bundle())
        if (state.usingGroup) {
            presenter.makeToastAlert("Sorry, this isn't supported in shared collections.")
            return
        }
        zoteroAPI.uploadNote(note)
    }

    override fun modifyNote(note: Note) {
        firebaseAnalytics.logEvent("modify_note", Bundle())
        if (state.usingGroup) {
            presenter.makeToastAlert("Sorry, this isn't supported in shared collections.")
            return
        }
        zoteroAPI.modifyNote(note, zoteroDBPicker.getZoteroDB().getLibraryVersion())
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
                zoteroDBPicker.getZoteroDB().deleteItem(note.key)
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
        zoteroAPI.deleteItem(item.ItemKey, item.version, object : DeleteItemListener {
            override fun success() {
                presenter.makeToastAlert("Successfully deleted your attachment.")
                zoteroDBPicker.getZoteroDB().deleteItem(item.ItemKey)
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

    override fun updateAttachment(item: Item, attachment: File) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun uploadAttachment(parent: Item, attachment: File) {
        zoteroAPI.uploadPDF(parent, attachment)
    }

    fun displayGroupsOnUI() {
        val groups = zoteroDatabase.getGroups()
        groups.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : MaybeObserver<List<GroupInfo>> {
                override fun onSuccess(groupInfo: List<GroupInfo>) {
                    Log.d("zotero", "complted")
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

        val modifiedSince = if (db.hasStorage()) {
            db.getLibraryVersion()
        } else {
            -1
        }
        val useCaching = modifiedSince != -1 // largely useless. a relic from before.

        val groupItemObservable =
            zoteroAPI.getItems(modifiedSince, useCaching, isGroup = true, groupID = group.id)
        groupItemObservable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : Observer<ZoteroAPIItemsResponse> {
                val items = LinkedList<Item>()
                var libraryVersion = -1
                override fun onComplete() {
                    Log.d(
                        "zotero",
                        "got ${this.items.size} many items in group. lv=$libraryVersion"
                    )
                    if (modifiedSince == -1) {
                        // these are all brand new items
                        db.items = items
                    } else {
                        //otherwise we just have changes and need to apply them.
                        db.loadItemsFromStorage()
                        db.applyChangesToItems(items)
                        presenter.makeToastAlert("Updated ${items.size} items.")
                    }
                    db.setItemsVersion(libraryVersion)
                    db.commitItemsToStorage()
                    if (db.collections != null) {
                        finishLoadingGroups(group)
                    }
                }

                override fun onSubscribe(d: Disposable) {
                    presenter.showLibraryLoadingAnimation()
                }

                override fun onNext(response: ZoteroAPIItemsResponse) {
                    assert(response.isCached == false)
                    this.libraryVersion = response.LastModifiedVersion
                    Log.d("zotero", "lv=${response.LastModifiedVersion}")
                    this.items.addAll(response.items)
                    presenter.updateLibraryRefreshProgress(this.items.size, response.totalResults)
                    Log.d("zotero", "got ${items.size} of ${response.totalResults} items")
                }

                override fun onError(e: Throwable) {
                    if (e is UpToDateException) {
                        db.loadItemsFromStorage()
                        if (db.collections != null) {
                            finishLoadingGroups(group)
                        }
                    } else {
                        Log.e("zotero", "${e}")
                        Log.e("zotero", "${e.stackTrace}")
                    }
                }
            })

        val groupCollectionsObservable =
            zoteroAPI.getCollections(modifiedSince, useCaching, isGroup = true, groupID = group.id)
        groupCollectionsObservable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : Observer<List<Collection>> {
                val collections = LinkedList<Collection>()
                override fun onComplete() {
                    db.collections = collections
                    db.commitCollectionsToStorage()
                    if (db.items != null) {
                        finishLoadingGroups(group)
                    }
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(collections: List<Collection>) {
                    this.collections.addAll(collections)
                }

                override fun onError(e: Throwable) {
                    if (e is UpToDateException) {
                        db.loadCollectionsFromStorage()
                        if (db.items != null) {
                            finishLoadingGroups(group)
                        }
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

                        if (db.hasStorage()) {
                            db.loadCollectionsFromStorage()
                            if (db.items != null) {
                                finishLoadingGroups(group)
                            }
                        }
                    }
                }
            })
    }

    private fun finishLoadingGroups(group: GroupInfo) {
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

    init {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val auth = AuthenticationStorage(context)
        if (auth.hasCredentials()) {
            zoteroAPI = ZoteroAPI(
                auth.getUserKey()!!,
                auth.getUserID()!!,
                auth.getUsername()!!
            )
        } else {
            presenter.createErrorAlert(
                "Error with stored API",
                "The API Key we have stored in the application is invalid!" +
                        "Please re-authenticate the application"
            ) { (context as Activity).finish() }
            auth.destroyCredentials()
            zoteroDBPicker.getZoteroDB().clearItemsVersion()
        }
    }
}