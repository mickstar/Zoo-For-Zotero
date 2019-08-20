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
import java.lang.Exception
import java.util.*

class LibraryActivityModel(private val presenter: Contract.Presenter, val context: Context) : Contract.Model {
    override fun openAttachment(item: Item) {
        var attachment : File? = null
        doAsync {
            try {
                attachment = zoteroAPI.downloadItem(context, item)
            }
            catch (exception :Exception){
                presenter.createErrorAlert("Error getting Attachment", "There was an error" +
                        "downloading the attachment ${item.data["filename"]} from the Zotero Servers.\n" +
                        "Error Message: ${exception.message}")
            }
            onComplete {
                presenter.stopLoading()
                if (attachment != null) {
                    presenter.openPDF(attachment!!)
                }
            }
        }
    }

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

    private lateinit var zoteroAPI: ZoteroAPI
    private var zoteroDB = ZoteroDB(context)

    override fun requestTestConnection() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestItems(onFinish : () -> (Unit)) {
        loadingItems = true
        zoteroAPI.getItems { statusCode, libraryVersion, items ->
            when (statusCode) {
                200 -> {
                    zoteroDB.items = items
                    zoteroDB.setItemsVersion(libraryVersion)
                    zoteroDB.commitItemsToStorage()
                }
                304 -> {
                    zoteroDB.loadItemsFromStorage()
                }
                404 -> {
                    if (!zoteroDB.hasStorage()) {
                        presenter.createErrorAlert(
                            "Network Error",
                            "There was a problem downloading your library from the Zotero API."
                        )
                        zoteroDB.items = LinkedList()
                    }
                }
            }
            loadingItems = false
            presenter.stopLoading()
            onFinish()
        }
    }

    override fun requestCollections(onFinish : () -> (Unit)) {
        loadingCollections = true
        zoteroAPI.getCollections { statusCode, collections ->
            when(statusCode) {
                200 -> {
                    zoteroDB.collections = collections
                    zoteroDB.commitCollectionsToStorage()
                }
                304 -> {
                    zoteroDB.loadCollectionsFromStorage()
                }
                404 -> {
                    if (!zoteroDB.hasStorage()) {
                        presenter.createErrorAlert(
                            "Network Error",
                            "There was a problem downloading your library from the Zotero API."
                        )
                        zoteroDB.collections = LinkedList()
                    }
                }
            }
            loadingCollections = false
            presenter.stopLoading()
            onFinish()
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
            )
            auth.destroyCredentials()
        }
    }
}