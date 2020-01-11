package com.mickstarify.zooforzotero.LibraryActivity

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
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
            SortMethod.AUTHOR -> it.getAuthor().toLowerCase(Locale.ROOT)
            SortMethod.DATE_ADDED -> it.getSortableDateAddedString()
        }
    }.thenBy { it.getTitle().toLowerCase(Locale.ROOT) }

    override fun openGroup(groupTitle: String) {
        model.getGroupByTitle(groupTitle)?.also {
            model.loadGroup(it)
        }

    }

    override fun startUploadingAttachment(attachment: Item) {
        view.showAttachmentUploadProgress(attachment)
    }

    override fun stopUploadingAttachment() {
        view.hideAttachmentUploadProgress()
        view.makeToastAlert("Finished uploading attachment.")
    }

    override fun askToUploadAttachments(changedAttachments: List<Item>) {
        // for the sake of sanity I will only ask to upload 1 attachment.
        // this is because of limitations of only having 1 upload occur concurrently
        // and my unwillingness to implement a chaining mechanism for uploads for what i expect to
        // be a niche power user.
        val attachment = changedAttachments.first()
        val fileSizeBytes = model.attachmentStorageManager.getFileSize(
            model.attachmentStorageManager.getAttachmentUri(attachment)
        )

        if (fileSizeBytes == 0L) {
            Log.e("zotero", "avoiding uploading a garbage PDF")
            FirebaseAnalytics.getInstance(model.context)
                .logEvent("AVOIDED_UPLOAD_GARBAGE", Bundle())
            model.removeFromRecentlyViewed(attachment)
        }

        val sizeKiloBytes = "${fileSizeBytes / 1000}KB"

        val message =
            "${attachment.data["filename"]!!} ($sizeKiloBytes) is different to Zotero's version. Would you like to upload this PDF to replace the remote version?"

        view.createYesNoPrompt(
            "Detected changes to attachment",
            message,
            "Upload",
            "No",
            { model.uploadAttachment(attachment) },
            { model.removeFromRecentlyViewed(attachment) })
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
        model.setCurrentCollection(collectionName)
        if (collectionName == "all" && !model.isUsingGroups()) {
            view.setTitle("My Library")
            val entries = model.getLibraryItems().sort().map { ListEntry(it) }
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        } else if (collectionName == "unfiled_items") {
            view.setTitle("Unfiled Items")
            val entries = model.getUnfiledItems().sort().map { ListEntry(it) }
            model.isDisplayingItems = entries.size > 0
            view.populateEntries(entries)
        } else if (collectionName == "group_all" && model.isUsingGroups()) {
            view.setTitle(model.getCurrentGroup().name)
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
            view.setTitle(collectionName)
            val entries = LinkedList<ListEntry>()
            entries.addAll(model.getSubCollections(collectionName).sortedBy {
                it.name.toLowerCase(Locale.ROOT)
            }.map { ListEntry(it) })

            entries.addAll(model.getItemsFromCollection(collectionName).sort().map { ListEntry(it) })
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

        //TODO i will delete this code next version. (just for version 2.2)
        if (model.preferences.firstRunForVersion27() && model.hasOldStorage()) {
            model.migrateFromOldStorage()
        } else {
            if (model.shouldIUpdateLibrary()) {
                model.loadGroups()
                model.downloadLibrary()
            } else {
                model.loadCollectionsLocally()
                model.loadItemsLocally()
                model.loadGroups()
            }
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

