package com.mickstarify.zooforzotero.LibraryActivity

import android.content.Context
import android.util.Log
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import java.io.File
import java.util.*

class LibraryActivityPresenter(val view: Contract.View, context: Context) : Contract.Presenter {
    override fun cancelAttachmentDownload() {
        model.cancelAttachmentDownload()
        view.hideAttachmentDownloadProgress()
    }

    override fun isShowingContent(): Boolean {
        return model.isDisplayingItems
    }

    override fun updateLibraryRefreshProgress(progress: Int, total: Int) {
        view.updateLibraryLoadingProgress(progress, total)
    }

    override fun closeQuery() {
        this.setCollection(model.currentCollection)
    }

    override fun filterEntries(query: String) {
        if (query == "" || !model.isLoaded()) {
            //not going to waste my time lol.
            return
        }
        val collections = model.filterCollections(query)
        val items = model.filterItems(query)

        val entries = LinkedList<ListEntry>()
        entries.addAll(collections.sortedBy { it.getName().toLowerCase(Locale.getDefault()) }
            .map { ListEntry(it) })
        entries.addAll(items.sortedBy { it.getTitle().toLowerCase(Locale.getDefault()) }
            .map { ListEntry(it) })

        view.populateEntries(entries)
    }

    override fun attachmentDownloadError() {
        view.hideAttachmentDownloadProgress()
        createErrorAlert(
            "Error getting Attachment", "There was an error " +
                    "downloading the attachment from the Zotero Servers.\n" +
                    "Please check your internet connection."
        ) { }
    }

    override fun openPDF(attachment: File) {
        view.hideAttachmentDownloadProgress()
        view.openPDF(attachment)
    }

    override fun openAttachment(item: Item) {
        view.updateAttachmentDownloadProgress(0, -1)
        model.openAttachment(item)
    }

    override fun updateAttachmentDownloadProgress(progress: Long, total: Long) {
        val progressKB = (progress / 1000).toInt()
        val totalKB = (total / 1000).toInt()
        view.updateAttachmentDownloadProgress(progressKB, totalKB)
    }


    override fun stopLoadingLibrary() {
        if (!model.loadingCollections && !model.loadingItems) {
            view.hideLoadingAnimation()
            view.hideLibraryContentDisplay()
        }
    }

    override fun requestLibraryRefresh() {
        view.showLoadingAnimation(showScreen = false)
        model.refreshLibrary()
    }

    override fun selectItem(item: Item) {
        if (item.getItemType() == "attachment") {
            this.openAttachment(item)
        } else {
            view.showItemDialog(item, model.getAttachments(item.ItemKey))
        }
    }

    override fun setCollection(collectionName: String) {
        Log.d("zotero", "Got request to change collection to ${collectionName}")
        model.currentCollection = collectionName
        if (collectionName == "all") {
            view.setTitle("My Library")
            val entries = model.getLibraryItems().sortedBy {
                it.getTitle().toLowerCase(Locale.getDefault())
            }.map { ListEntry(it) }
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        } else {
            view.setTitle(collectionName)

            val entries = LinkedList<ListEntry>()
            entries.addAll(model.getSubCollections(collectionName).sortedBy {
                it.getName().toLowerCase(Locale.getDefault())
            }.map { ListEntry(it) })
            entries.addAll(model.getItemsFromCollection(collectionName).sortedBy {
                it.getTitle().toLowerCase(Locale.getDefault())
            }.map { ListEntry(it) })
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        }
    }

    override fun testConnection() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun receiveCollections(collections: List<Collection>) {
        for (collection: Collection in collections.filter {
            !it.hasParent()
        }.sortedBy { it.getName().toLowerCase(Locale.getDefault()) }) {
            Log.d("zotero", "Got collection ${collection.getName()}")
            view.addNavigationEntry(collection, "Catalog")
        }
    }

    override fun createErrorAlert(
        title: String,
        message: String,
        onClick: () -> Unit
    ) {
        view.createErrorAlert(title, message, onClick)
    }

    override fun makeToastAlert(message: String) {
        view.makeToastAlert(message)
    }

    private val model = LibraryActivityModel(this, context)

    init {
        view.initUI()
        view.showLoadingAnimation(true)
        view.showLibraryContentDisplay("Loading your library content.")
        if (model.shouldIUpdateLibrary()) {
            model.requestCollections({ receiveCollections(model.getCollections()) })
            model.requestItems({ this.setCollection("all") })
        } else {
            model.loadCollectionsLocally { receiveCollections(model.getCollections()) }
            model.loadItemsLocally { this.setCollection("all") }
        }
    }
}