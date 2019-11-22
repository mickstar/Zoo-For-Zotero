package com.mickstarify.zooforzotero.LibraryActivity

import android.content.Context
import android.util.Log
import com.mickstarify.zooforzotero.SortMethod
import com.mickstarify.zooforzotero.ZoteroAPI.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import java.io.File
import java.util.*

class LibraryActivityPresenter(val view: Contract.View, context: Context) : Contract.Presenter {
    val sortMethod = compareBy<Item> {
        when (model.preferences.getSortMethod()) {
            SortMethod.TITLE -> it.getTitle().toLowerCase(Locale.getDefault())
            SortMethod.DATE -> it.getSortableDateString()
            SortMethod.AUTHOR -> it.getAuthor().toLowerCase(Locale.getDefault())
            SortMethod.DATE_ADDED -> it.getSortableDateAddedString()
        }
    }.thenBy { it.getTitle().toLowerCase(Locale.getDefault()) }

    override fun openGroup(groupTitle: String) {
        model.getGroupByTitle(groupTitle)?.also {
            model.loadGroup(it)
        }

    }

    override fun displayGroupsOnActionBar(groups: List<GroupInfo>) {
        groups.forEach { groupInfo: GroupInfo ->
            Log.d("zotero", "got group ${groupInfo.name}")
            view.addSharedCollection(groupInfo)
        }
    }

    override fun modifyNote(note: Note) {
        model.modifyNote(note)
    }

    override fun createNote(note: Note) {
        if (note.note.trim() != "") {
            model.createNote(note)
        }
    }

    override fun deleteNote(note: Note) {
        model.deleteNote(note)
    }

    override fun redisplayItems() {
        if (model.isLoaded()) {
            if (model.currentCollection != "unset") {
                setCollection(model.currentCollection, isSubCollection = true)
            }
        }
    }

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
        val items = model.filterItems(query).sort()

        val entries = LinkedList<ListEntry>()
        entries.addAll(collections.sortedBy { it.getName().toLowerCase(Locale.getDefault()) }
            .map { ListEntry(it) })
        entries.addAll(
            items
                .map { ListEntry(it) })

        view.populateEntries(entries)
    }

    override fun attachmentDownloadError(message: String) {
        view.hideAttachmentDownloadProgress()
        if (message == "") {
            createErrorAlert(
                "Error getting Attachment", "There was an error " +
                        "downloading the attachment from the Zotero Servers.\n" +
                        "Please check your internet connection."
            ) { }
        } else {
            createErrorAlert("Error getting Attachment", message) { }
        }
    }

    override fun openPDF(attachment: File) {
        view.hideAttachmentDownloadProgress()
        model.openPDF(attachment)
    }

    override fun openAttachment(item: Item) {
        view.updateAttachmentDownloadProgress(0, -1)
        model.downloadAttachment(item)
    }

    override fun updateAttachmentDownloadProgress(progress: Long, total: Long) {
        val progressKB = (progress / 1000).toInt()
        val totalKB = (total / 1000).toInt()
        view.updateAttachmentDownloadProgress(progressKB, totalKB)
    }


    override fun showLibraryLoadingAnimation() {
        view.showLoadingAnimation(showScreen = true)
        view.setTitle("Loading")
    }

    override fun hideLibraryLoadingAnimation() {
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
        } else if (item.getItemType() == "note") {
            //ignore
        } else {
            model.selectedItem = item
            view.showItemDialog(
                item,
                model.getAttachments(item.ItemKey),
                model.getNotes(item.ItemKey)
            )
        }
    }

    override fun refreshItemView() {
        val item = model.selectedItem
        if (item != null) {
            view.closeItemView()
            view.showItemDialog(
                item,
                model.getAttachments(item.ItemKey),
                model.getNotes(item.ItemKey)
            )
        }
    }

    override fun setCollection(collectionName: String, isSubCollection: Boolean) {
        /*SetCollection is the method used to display items on the listView. It
        * has to get the data, then sort it, then provide it to the view.*/
        if (!model.isLoaded()) {
            Log.e("zotero", "tried to change collection before fully loaded!")
            return
        }
        // this check covers if the user has just left their group library from the sidemenu.
        if (!isSubCollection) {
            model.usePersonalLibrary()
        }

        Log.d("zotero", "Got request to change collection to ${collectionName}")
        model.currentCollection = collectionName
        if (collectionName == "all" && model.usingGroup == false) {
            view.setTitle("My Library")
            val entries = model.getLibraryItems().sort().map { ListEntry(it) }
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        } else if (collectionName == "unfiled_items") {
            view.setTitle("Unfiled Items")
            val entries = model.getUnfiledItems().sort().map { ListEntry(it) }
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        } else if (collectionName == "group_all" && model.usingGroup) {
            view.setTitle(model.currentGroup!!.name)
            val entries = LinkedList<ListEntry>()
            entries.addAll(model.getCollections().filter {
                !it.hasParent()
            }.sortedBy {
                it.getName().toLowerCase(Locale.getDefault())
            }.map { ListEntry(it) })
            entries.addAll(model.getLibraryItems().sort().map { ListEntry(it) })
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        }
        // It is an actual collection on the user's private.
        else {
            view.setTitle(collectionName)
            val entries = LinkedList<ListEntry>()
            entries.addAll(model.getSubCollections(collectionName).sortedBy {
                it.getName().toLowerCase(Locale.getDefault())
            }.map { ListEntry(it) })

            entries.addAll(model.getItemsFromCollection(collectionName).sort().map { ListEntry(it) })
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        }
    }

    override fun testConnection() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun receiveCollections(collections: List<Collection>) {
        view.clearSidebar()
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
            model.requestCollections()
            model.requestItems()
            model.loadGroups()
        } else {
            model.loadCollectionsLocally()
            model.loadItemsLocally()
            model.loadGroups()
        }
    }

    private fun List<Item>.sort(): List<Item> {
        if (model.preferences.isSortedAscendingly()) {
            return this.sortedWith(sortMethod)
        }
        return this.sortedWith(sortMethod).reversed()
    }
}

