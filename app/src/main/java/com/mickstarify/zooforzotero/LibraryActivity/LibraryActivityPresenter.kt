package com.mickstarify.zooforzotero.LibraryActivity

import android.content.Context
import android.util.Log
import com.mickstarify.zooforzotero.SortMethod
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import java.util.*

class LibraryActivityPresenter(val view: Contract.View, context: Context) : Contract.Presenter {
    val sortMethod = compareBy<Item> {
        when (model.preferences.getSortMethod()) {
            SortMethod.TITLE -> it.getTitle().toLowerCase(Locale.ROOT)
            SortMethod.DATE -> it.getSortableDateString()
            SortMethod.DATE_ADDED -> it.getSortableDateAddedString()
            SortMethod.AUTHOR -> {
                val authorText = it.getAuthor().toLowerCase(Locale.ROOT)
                // force empty authors to the bottom. Just like the zotero desktop client.
                if (authorText == ""){
                    "zzz"
                } else {
                    authorText
                }
            }
        }
    }.thenBy { it.getTitle().toLowerCase(Locale.ROOT) }

    override fun openGroup(groupTitle: String) {
        model.getGroupByTitle(groupTitle)?.also {
            model.loadGroup(it)
        }

    }

    override fun startUploadingAttachmentProgress(attachment: Item) {
        view.showAttachmentUploadProgress(attachment)
    }

    override fun stopUploadingAttachmentProgress() {
        view.hideAttachmentUploadProgress()
        view.makeToastAlert("Finished uploading attachment.")
    }

    override fun onResume() {
        if (model.isLoaded()) {
            model.checkAttachmentStorageAccess()
            model.checkAllAttachmentsForModification()
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
            if (model.getCurrentCollection() != "unset") {
                setCollection(model.getCurrentCollection(), isSubCollection = true)
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

    override fun updateLibraryRefreshProgress(
        progress: Int,
        total: Int,
        message: String
    ) {
        view.updateLibraryLoadingProgress(progress, total, message)
    }

    override fun closeQuery() {
        this.setCollection(model.getCurrentCollection())
    }

    override fun addFilterState(query: String) {
        /* This method tells the model to add a filtered state to the the states stack
        * This will allow a user to use the back button to return to the library before
        * the filter was initiated. The need for this method is a little contrived but I had to hack
        * the functionality in so forgive me. */

        val oldState = model.state
        model.states.add(LibraryModelState().apply {
            this.currentGroup = oldState.currentGroup
            this.currentCollection = oldState.currentCollection
            this.filterText = query
        })
    }

    override fun filterEntries(query: String) {
        if (query == "" || !model.isLoaded()) {
            //not going to waste my time lol.
            return
        }
        val collections = model.filterCollections(query)
        val items = model.filterItems(query).sort()

        val entries = LinkedList<ListEntry>()
        entries.addAll(collections.sortedBy { it.name.toLowerCase(Locale.ROOT) }
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

    override fun finishDownloadingAttachment() {
        view.hideAttachmentDownloadProgress()
    }

    override fun createYesNoPrompt(
        title: String,
        message: String,
        yesText: String,
        noText: String,
        onYesClick: () -> Unit,
        onNoClick: () -> Unit
    ) {
        view.createYesNoPrompt(title, message, yesText, noText, onYesClick, onNoClick)
    }

    override fun showBasicSyncAnimation() {
        view.showBasicSyncAnimation()
    }

    override fun hideBasicSyncAnimation() {
        view.hideBasicSyncAnimation()
    }

    override fun openAttachment(item: Item) {
        model.openAttachment(item)
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

    override fun selectItem(
        item: Item,
        longPress: Boolean
    ) {
        Log.d("zotero", "pressed ${item.itemType}")
        if (item.itemType == "attachment") {
            this.openAttachment(item)
        } else if (item.itemType == "note") {
            val note = Note(item)
            view.showNote(note)
        } else {
            val itemAttachments = model.getAttachments(item.itemKey)
            if (!longPress && model.preferences.shouldOpenPDFOnOpen()) {
                val pdfAttachment =
                    itemAttachments.filter { it.data["contentType"] == "application/pdf" }
                        .firstOrNull()
                if (pdfAttachment != null) {
                    openAttachment(pdfAttachment)
                    return
                }
                // otherwise there is no PDF and we will continue and just open the itemview.
            }

            model.selectedItem = item
            view.showItemDialog(
                item,
                itemAttachments,
                model.getNotes(item.itemKey)
            )
        }
    }

    override fun refreshItemView() {
        val item = model.selectedItem
        if (item != null) {
            view.closeItemView()
            view.showItemDialog(
                item,
                model.getAttachments(item.itemKey),
                model.getNotes(item.itemKey)
            )
        }
    }

//    override fun openSubcollection(collection: Collection){
//        if (!model.isLoaded()){
//            Log.e("zotero", "tried to change collection before fully loaded!")
//            return
//        }
//        model.setCurrentCollection(collection.name)
//
//    }

    override fun openTrash() {
        if (!model.isLoaded()) {
            Log.e("zotero", "tried to change collection before fully loaded!")
            return
        }
        model.usePersonalLibrary()
        view.setTitle("Trash")
        val entries = model.getTrashedItems().sort().map{ListEntry(it)}
        model.isDisplayingItems = entries.size > 0
        model.setCurrentCollection("zooforzotero_Trash")
        view.populateEntries(entries)
    }

    override fun uploadAttachment(item: Item) {
        model.uploadAttachment(item)
    }

    override fun requestForceResync() {
        model.destroyLibrary()
    }

    override fun backButtonPressed() {
        model.loadPriorState()
        view.highlightMenuItem(model.state)
    }

    override fun setCollection(collectionKey: String, isSubCollection: Boolean) {
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

        Log.d("zotero", "Got request to change collection to ${collectionKey}")
        model.setCurrentCollection(collectionKey, !isSubCollection)
        if (collectionKey == "all" && !model.isUsingGroups()) {
            view.setTitle("My Library")
            val entries = model.getLibraryItems().sort().map { ListEntry(it) }
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        } else if (collectionKey == "unfiled_items") {
            view.setTitle("Unfiled Items")
            val entries = model.getUnfiledItems().sort().map { ListEntry(it) }
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        } else if (collectionKey == "zooforzotero_Trash"){
            this.openTrash()
        } else if (collectionKey == "group_all" && model.isUsingGroups()) {
            view.setTitle(model.getCurrentGroup()?.name ?: "ERROR")
            val entries = LinkedList<ListEntry>()
            entries.addAll(model.getCollections().filter {
                !it.hasParent()
            }.sortedBy {
                it.name.toLowerCase(Locale.ROOT)
            }.map { ListEntry(it) })
            entries.addAll(model.getLibraryItems().sort().map { ListEntry(it) })
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        }
        // It is an actual collection on the user's private.
        else {
            val collection = model.getCollectionFromKey(collectionKey)

            view.setTitle(collection?.name ?: "Unknown Collection")
            val entries = LinkedList<ListEntry>()
            entries.addAll(model.getSubCollections(collectionKey).sortedBy {
                it.name.toLowerCase(Locale.ROOT)
            }.map { ListEntry(it) })

            entries.addAll(model.getItemsFromCollection(collectionKey).sort().map { ListEntry(it) })
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        }
    }

    override fun receiveCollections(collections: List<Collection>) {
        view.clearSidebar()
        for (collection: Collection in collections.filter {
            !it.hasParent()
        }.sortedBy { it.name.toLowerCase(Locale.ROOT) }) {
            Log.d("zotero", "Got collection ${collection.name}")
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
            model.loadGroups()
            model.downloadLibrary()
        } else {
            model.loadLibraryLocally()
            model.loadGroups()
        }
    }

    // extension function to sort lists of items
    private fun List<Item>.sort(): List<Item> {
        if (model.preferences.isSortedAscendingly()) {
            return this.sortedWith(sortMethod)
        }
        return this.sortedWith(sortMethod).reversed()
    }
}

