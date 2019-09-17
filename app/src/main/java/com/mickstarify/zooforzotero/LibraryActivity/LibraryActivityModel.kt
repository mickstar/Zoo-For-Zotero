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
import com.mickstarify.zooforzotero.SortMethod
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import com.mickstarify.zooforzotero.ZoteroAPI.*
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import java.io.File
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class LibraryActivityModel(private val presenter: Contract.Presenter, val context: Context) :
    Contract.Model {
    // stores the current item being viewed by the user. (useful for refreshing the view)
    var selectedItem: Item? = null
    // just a flag to store whether we have shown the user a network error so that we don't
    // do it twice (from getCatalog & getItems
    private var shownNetworkError: Boolean = false
    var currentCollection: String = "unset"

    var loadingItems = false
    var loadingCollections = false

    private var itemsDownloadAttempt = 0
    private var collectionsDownloadAttempt = 0

    private var firebaseAnalytics: FirebaseAnalytics

    var isDisplayingItems = false

    val preferences = PreferenceManager(context)

    override fun refreshLibrary() {
        this.requestItems({}, useCaching = true)
        this.requestCollections({}, useCaching = true)
    }

    fun shouldIUpdateLibrary(): Boolean {
        if (!zoteroDB.hasStorage()) {
            return true
        }
        val currentTimestamp = System.currentTimeMillis()
        val lastModified = zoteroDB.getLastModifiedTimestamp()

        if (TimeUnit.MILLISECONDS.toHours(currentTimestamp - lastModified) >= 24) {
            return true
        }
        return false
    }

    val sortMethod = compareBy<Item> {
        when (preferences.getSortMethod()) {
            SortMethod.TITLE -> it.getTitle().toLowerCase(Locale.getDefault())
            SortMethod.DATE -> it.getSortableDateString()
            SortMethod.AUTHOR -> it.getAuthor().toLowerCase(Locale.getDefault())
            SortMethod.DATE_ADDED -> it.getSortableDateAddedString()
        }
    }.thenBy { it.getTitle().toLowerCase(Locale.getDefault()) }

    override fun isLoaded(): Boolean {
        return zoteroDB.isPopulated()
    }

    override fun getLibraryItems(): List<Item> {
        return zoteroDB.getDisplayableItems().sortedWith(sortMethod)
    }

    override fun getItemsFromCollection(collectionName: String): List<Item> {
        val collectionKey = zoteroDB.getCollectionId(collectionName)
        if (collectionKey != null) {
            return zoteroDB.getItemsFromCollection(collectionKey).sortedWith(sortMethod)
        }
        throw (Exception("Error, could not find collection with name ${collectionName}"))
    }

    override fun getSubCollections(collectionName: String): List<Collection> {
        val collectionKey = zoteroDB.getCollectionId(collectionName)
        if (collectionKey != null) {
            return zoteroDB.getSubCollectionsFor(collectionKey)
        }
        throw (Exception("Error, could not find collection with name ${collectionName}"))
    }

    private lateinit var zoteroAPI: ZoteroAPI
    private var zoteroDB = ZoteroDB(context)

    fun loadItemsLocally(onFinish: () -> Unit) {
        doAsync {
            zoteroDB.loadItemsFromStorage()
            onComplete {
                finishGetItems(onFinish)
            }
        }
    }

    override fun requestItems(onFinish: () -> (Unit), useCaching: Boolean) {
        loadingItems = true
        itemsDownloadAttempt++

        zoteroAPI.getItems(
            useCaching,
            zoteroDB.getLibraryVersion(),
            object : ZoteroAPIDownloadItemsListener {
                override fun onDownloadComplete(items: List<Item>, libraryVersion: Int) {
                    zoteroDB.writeDatabaseUpdatedTimestamp()
                    zoteroDB.items = items
                    zoteroDB.setItemsVersion(libraryVersion)
                    zoteroDB.commitItemsToStorage()
                    finishGetItems(onFinish)
                }

                override fun onCachedComplete() {
                    zoteroDB.writeDatabaseUpdatedTimestamp()
                    try {
                        zoteroDB.loadItemsFromStorage()
                    } catch (e: Exception) {
                        Log.d("zotero", "there was an error loading cached items copy.")
                        presenter.makeToastAlert("There was an error loading the cached copy of your library")
                        requestItems(onFinish, useCaching = false)
                    }
                    finishGetItems(onFinish)
                }

                override fun onNetworkFailure() {
                    if (itemsDownloadAttempt < 2) {
                        Log.d("zotero", "attempting another download of items")
                        requestItems(onFinish, useCaching)
                    } else if (zoteroDB.hasStorage()) {
                        Log.d("zotero", "no internet connection. Using cached copy")
                        presenter.makeToastAlert("No internet connection, using cached copy of library")
                        zoteroDB.loadItemsFromStorage()
                    } else {
                        if (shownNetworkError == false) {
                            shownNetworkError = true
                            presenter.createErrorAlert(
                                "Network Error",
                                "There was a problem downloading your library from the Zotero API.\n" +
                                        "Please check your network connection and try again.",
                                { shownNetworkError = false }
                            )
                        }
                        zoteroDB.items = LinkedList()
                    }
                    finishGetItems(onFinish)
                }

                override fun onProgressUpdate(progress: Int, total: Int) {
                    Log.d("zotero", "updating items, got $progress of $total")
                    presenter.updateLibraryRefreshProgress(progress, total)
                }
            })
    }

    private fun finishGetItems(onFinish: () -> Unit) {
        if (zoteroDB.items != null) {
            loadingItems = false
            if (!loadingCollections) {
                presenter.stopLoadingLibrary()
            }
            onFinish()
        }
    }

    override fun loadCollectionsLocally(onFinish: () -> Unit) {
        doAsync {
            zoteroDB.loadCollectionsFromStorage()
            onComplete {
                finishGetCollections(onFinish)
            }
        }
    }

    override fun requestCollections(onFinish: () -> (Unit), useCaching: Boolean) {
        loadingCollections = true
        collectionsDownloadAttempt++

        zoteroAPI.getCollections(
            useCaching,
            zoteroDB.getLibraryVersion(),
            object : ZoteroAPIDownloadCollectionListener {
                override fun onCachedComplete() {
                    try {
                        zoteroDB.loadCollectionsFromStorage()
                    } catch (e: Exception) {
                        Log.d("zotero", "there was an error loading cached collections copy.")
                        requestCollections(onFinish, useCaching = false)
                    }
                    finishGetCollections(onFinish)
                }


                override fun onDownloadComplete(collections: List<Collection>) {
                    zoteroDB.collections = collections
                    zoteroDB.commitCollectionsToStorage()
                    finishGetCollections(onFinish)
                }

                override fun onNetworkFailure() {
                    if (collectionsDownloadAttempt < 2) {
                        Log.d("zotero", "attempting another download of collections")
                        requestCollections(onFinish, useCaching)
                    } else if (zoteroDB.hasStorage()) {
                        Log.d("zotero", "no internet connection. Using cached copy")
                        zoteroDB.loadCollectionsFromStorage()
                    } else {
                        if (shownNetworkError == false) {
                            shownNetworkError = true
                            presenter.createErrorAlert(
                                "Network Error",
                                "There was a problem downloading your library from the Zotero API.\n" +
                                        "Please check your network connection and try again.",
                                { shownNetworkError = false }
                            )
                        }
                        zoteroDB.collections = LinkedList()
                    }
                    finishGetCollections(onFinish)
                }
            })
    }

    private fun finishGetCollections(onFinish: () -> Unit) {
        if (zoteroDB.collections != null) {
            loadingCollections = false
            if (!loadingItems) {
                presenter.stopLoadingLibrary()
            }
            onFinish()
        }
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
        val queryUpper = query.toUpperCase(Locale.getDefault())
        return zoteroDB.collections?.filter {
            it.getName().toUpperCase(Locale.getDefault()).contains(queryUpper)
        } ?: LinkedList()
    }

    override fun openPDF(attachment: File) {
        var intent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var uri: Uri?
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
        val items = zoteroDB.items?.filter {
            it.query(query)

        } ?: LinkedList()

        items.sortedWith(sortMethod)

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
            object : ZoteroAPIDownloadAttachmentListener {
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

                override fun onFailure() {
                    presenter.attachmentDownloadError()
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

    override fun cancelAttachmentDownload() {
        this.isDownloading = false
        task?.cancel(true)
    }

    override fun createNote(note: Note) {
        zoteroAPI.uploadNote(note)
    }

    override fun modifyNote(note: Note) {
        zoteroAPI.modifyNote(note, zoteroDB.getLibraryVersion())
    }

    override fun deleteNote(note: Note) {
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
            zoteroDB.clearItemsVersion()
        }
    }
}