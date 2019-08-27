package com.mickstarify.zooforzotero.LibraryActivity

import android.content.Context
import android.util.Log
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroAPI
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroDB
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import java.io.File
import java.util.*

class LibraryActivityModel(private val presenter: Contract.Presenter, val context: Context) : Contract.Model {
    override fun openAttachment(item: Item) {
        var attachment : File? = null
        doAsync {
            try {
                attachment = zoteroAPI.downloadItem(context, item)
            }
            catch (exception :Exception){
                presenter.createErrorAlert(
                    "Error getting Attachment", "There was an error" +
                            "downloading the attachment ${item.data["filename"]} from the Zotero Servers.\n" +
                            "Error Message: ${exception.message}"
                ) { }
            }
            onComplete {
                if (attachment != null) {
                    presenter.openPDF(attachment!!)
                }
            }
        }
    }

    // just a flag to store whether we have shown the user a network error so that we don't
    // do it twice (from getCatalog & getItems
    private var shownNetworkError: Boolean = false

    var loadingItems = false
    var loadingCollections = false
    override fun refreshLibrary() {
        this.requestItems({})
        this.requestCollections({})
    }

    override fun getLibraryItems(): List<Item> {
        return zoteroDB.getDisplayableItems()
    }

    override fun getItemsFromCollection(collectionName: String): List<Item> {
        val collectionKey =  zoteroDB.getCollectionId(collectionName)
        if (collectionKey != null){
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

    override fun requestItems(onFinish : () -> (Unit)) {
        loadingItems = true
        val useCaching = zoteroDB.hasStorage()
        zoteroAPI.getItems(useCaching) { statusCode, libraryVersion, items ->
            when (statusCode) {
                200 -> {
                    zoteroDB.items = items
                    zoteroDB.setItemsVersion(libraryVersion)
                    zoteroDB.commitItemsToStorage()
                }
                304 -> {
                    try {
                        zoteroDB.loadItemsFromStorage()
                    } catch (e: Exception) {
                        Log.d("zotero", "there was an error loading cached items copy.")
                        this.requestItems(onFinish)
                    }
                }
                404 -> {
                    if (zoteroDB.hasStorage()) {
                        Log.d("zotero", "no internet connection. Using cached copy")
                        presenter.makeToastAlert("No internet connection, using cached copy of library")
                        zoteroDB.loadItemsFromStorage()
                    } else {
                        if (this.shownNetworkError == false) {
                            this.shownNetworkError = true
                            presenter.createErrorAlert(
                                "Network Error",
                                "There was a problem downloading your library from the Zotero API.\n" +
                                        "Please check your network connection and try again.",
                                { this.shownNetworkError = false }
                            )
                        }
                        zoteroDB.items = LinkedList()
                    }
                }
            }
            if (zoteroDB.items != null) {
                loadingItems = false
                if (!loadingCollections) {
                    presenter.stopLoading()
                }
                onFinish()
            }
        }
    }

    override fun requestCollections(onFinish : () -> (Unit)) {
        loadingCollections = true
        val useCaching = zoteroDB.hasStorage()
        zoteroAPI.getCollections(useCaching) { statusCode, collections ->
            when(statusCode) {
                200 -> {
                    zoteroDB.collections = collections
                    zoteroDB.commitCollectionsToStorage()
                }
                304 -> {
                    try {
                        zoteroDB.loadCollectionsFromStorage()
                    } catch (e: Exception) {
                        Log.d("zotero", "there was an error loading cached collections copy.")
                        this.requestCollections(onFinish)
                    }
                }
                404 -> {
                    if (zoteroDB.hasStorage()) {
                        Log.d("zotero", "no internet connection. Using cached copy")
                        zoteroDB.loadCollectionsFromStorage()
                    } else {
                        if (this.shownNetworkError == false) {
                            this.shownNetworkError = true
                            presenter.createErrorAlert(
                                "Network Error",
                                "There was a problem downloading your library from the Zotero API.\n" +
                                        "Please check your network connection and try again.",
                                { this.shownNetworkError = false }
                            )
                        }
                        zoteroDB.collections = LinkedList()
                    }
                }
            }
            if (zoteroDB.collections != null) {
                loadingCollections = false
                if (!loadingItems) {
                    presenter.stopLoading()
                }
                onFinish()
            }
        }
    }

    override fun getCollections() : List<Collection>? {
        return zoteroDB.collections
    }

    override fun requestItemsForCollection(collectionKey: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestItem() {
        TODO("bro")
    }

    override fun getAttachments(itemKey : String) : List<Item>{
        return zoteroDB.getAttachments(itemKey)
    }

    init {
        val auth = AuthenticationStorage(context)
        if (auth.hasCredentials()) {
            zoteroAPI = ZoteroAPI(
                auth.getUserKey()!!,
                auth.getUserID()!!,
                auth.getUsername()!!,
                zoteroDB.getItemsVersion()
            )
        } else {
            presenter.createErrorAlert(
                "Error with stored API", "The API Key we have stored in the application is invalid!" +
                        "Please re-authenticate the application"
            ) { }
            auth.destroyCredentials()
        }
    }
}