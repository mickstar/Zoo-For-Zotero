package com.mickstarify.zooforzotero.LibraryActivity

import android.content.Context
import android.util.Log
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import com.mickstarify.zooforzotero.ZoteroAPI.*
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import java.io.File
import java.util.*

class LibraryActivityModel(private val presenter: Contract.Presenter, val context: Context) :
    Contract.Model {
    // just a flag to store whether we have shown the user a network error so that we don't
    // do it twice (from getCatalog & getItems
    private var shownNetworkError: Boolean = false
    var currentCollection: String = "unset"

    var loadingItems = false
    var loadingCollections = false

    private var itemsDownloadAttempt = 0
    private var collectionsDownloadAttempt = 0

    var isDisplayingItems = false

    override fun refreshLibrary() {
        this.requestItems({}, useCaching = true)
        this.requestCollections({}, useCaching = true)
    }

    override fun isLoaded(): Boolean {
        return !(zoteroDB.items == null || zoteroDB.collections == null)
    }

    override fun getLibraryItems(): List<Item> {
        return zoteroDB.getDisplayableItems()
    }

    override fun getItemsFromCollection(collectionName: String): List<Item> {
        val collectionKey = zoteroDB.getCollectionId(collectionName)
        if (collectionKey != null) {
            return zoteroDB.getItemsFromCollection(collectionKey)
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

    override fun requestTestConnection() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestItems(onFinish: () -> (Unit), useCaching: Boolean) {
        loadingItems = true
        itemsDownloadAttempt++

        zoteroAPI.getItems(
            useCaching,
            zoteroDB.getLibraryVersion(),
            object : ZoteroAPIDownloadItemsListener {
                override fun onCachedComplete() {
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

                override fun onDownloadComplete(items: List<Item>, libraryVersion: Int) {
                    zoteroDB.items = items
                    zoteroDB.setItemsVersion(libraryVersion)
                    zoteroDB.commitItemsToStorage()
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
                        finishGetCollections(onFinish)
                    }
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

    override fun requestItemsForCollection(collectionKey: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestItem() {
        TODO("bro")
    }

    override fun getAttachments(itemKey: String): List<Item> {
        return zoteroDB.getAttachments(itemKey)
    }

    override fun filterCollections(query: String): List<Collection> {
        val queryUpper = query.toUpperCase(Locale.getDefault())
        return zoteroDB.collections?.filter {
            it.getName().toUpperCase(Locale.getDefault()).contains(queryUpper)
        } ?: LinkedList()
    }

    override fun filterItems(query: String): List<Item> {
        val queryUpper = query.toUpperCase(Locale.getDefault())
        return zoteroDB.items?.filter {
            (it.ItemKey.toUpperCase(Locale.getDefault()).contains(queryUpper) ||
                    it.getTitle().toUpperCase(Locale.getDefault()).contains(queryUpper) ||
                    it.getItemType().toUpperCase(Locale.getDefault()).contains(queryUpper) ||
                    it.tags.joinToString("_").toUpperCase(Locale.getDefault()).contains(query))

        } ?: LinkedList()
    }

    override fun openAttachment(item: Item) {
        zoteroAPI.downloadItem(context, item, object : ZoteroAPIDownloadAttachmentListener {
            override fun onNetworkFailure() {
                presenter.attachmentDownloadError()
            }

            override fun onComplete(attachment: File) {
                presenter.openPDF(attachment)

            }

            override fun onFailure() {
                presenter.attachmentDownloadError()
            }

            override fun onProgressUpdate(progress: Long, total: Long) {
                Log.d("zotero", "Downloading attachment. got $progress of $total")
                presenter.updateAttachmentDownloadProgress(progress, total)
            }
        })
    }

    init {
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
            ) { }
            auth.destroyCredentials()
            zoteroDB.clearItemsVersion()
        }
    }
}