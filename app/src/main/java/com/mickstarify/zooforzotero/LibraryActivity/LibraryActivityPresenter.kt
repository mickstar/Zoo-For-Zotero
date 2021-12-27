package com.mickstarify.zooforzotero.LibraryActivity

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.mickstarify.zooforzotero.LibraryActivity.ViewModels.LibraryListViewModel
import com.mickstarify.zooforzotero.LibraryActivity.ViewModels.LibraryLoadingScreenViewModel
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.SortMethod
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import java.util.LinkedList
import java.util.Locale

class LibraryActivityPresenter(val view: LibraryActivity, context: Context) : Contract.Presenter {
    override lateinit var libraryListViewModel: LibraryListViewModel
    override lateinit var libraryLoadingViewModel: LibraryLoadingScreenViewModel

    val sortMethod = compareBy<Item> {
        when (model.preferences.getSortMethod()) {
            SortMethod.TITLE -> it.getTitle().toLowerCase(Locale.ROOT)
            SortMethod.DATE -> it.getSortableDateString()
            SortMethod.DATE_ADDED -> it.getSortableDateAddedString()
            SortMethod.AUTHOR -> {
                val authorText = it.getAuthor().toLowerCase(Locale.ROOT)
                // force empty authors to the bottom. Just like the zotero desktop client.
                if (authorText == "") {
                    "zzz"
                } else {
                    authorText
                }
            }
        }
    }.thenBy { it.getTitle().toLowerCase(Locale.ROOT) }

    override fun openGroup(groupTitle: String) {
        model.getGroupByTitle(groupTitle)?.also {
            model.startGroupSync(it)
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
                setCollection(model.getCurrentCollection())
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
        Log.d("zotero", "Updating library loading progress.")

        if (total > 25 && view.getCurrentScreen() != AvailableScreens.LIBRARY_LOADING_SCREEN) {
            view.navController.navigate(R.id.libraryLoadingScreen)
        }

        libraryLoadingViewModel.setAmountOfDownloadedEntries(progress)
        libraryLoadingViewModel.setTotalAmountOfEntries(total)
        libraryLoadingViewModel.setLoadingMessage(message)

//        view.updateLibraryLoadingProgress(progress, total, message)
    }

    override fun isLiveSearchEnabled(): Boolean {
        return model.preferences.shouldLiveSearch()
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

    override fun onTagOpen(tagName: String) {
        this.filterEntries("tag:${tagName}")
        this.addFilterState("tag:${tagName}")
        view.setTitle("Tag: $tagName")
    }

    override fun showLoadingAlertDialog(message: String) {
        view.showLoadingAlertDialog(message)
    }

    override fun hideLoadingAlertDialog() {
        view.hideLoadingAlertDialog()
    }

    override fun filterEntries(query: String) {
        Log.d("zotero", "filtering $query")
        if (query == "" || !model.isLoaded()) {
            //not going to waste my time lol.
            return
        }

        if (query.startsWith("tag:")) {
            val tagName = query.substring(4) // remove tag:
            val entries = model.getItemsForTag(tagName).map { ListEntry(it) }
            libraryListViewModel.setItems(entries)
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

        Log.d("zotero", "setting items ${entries.size}")
        libraryListViewModel.setItems(entries)
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
        libraryListViewModel.setIsShowingLoadingAnimation(true)
    }

    override fun hideBasicSyncAnimation() {
        libraryListViewModel.setIsShowingLoadingAnimation(false)
        // we are finished loading and should hide the loading screen.
        view.navController.navigate(R.id.libraryListFragment)
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
//        view.showLoadingAnimation(showScreen = true)
        view.setTitle("Loading")

        (view as LibraryActivity).navController.navigate(R.id.libraryLoadingScreen)
    }

    override fun hideLibraryLoadingAnimation() {
//        view.hideLoadingAnimation()
//        view.hideLibraryContentDisplay()
        Log.d("zotero", "loading library list fragment")
        (view as LibraryActivity).navController.navigate(R.id.libraryListFragment)
    }

    override fun requestLibraryRefresh() {
        libraryListViewModel.setIsShowingLoadingAnimation(true)
        model.refreshLibrary(useSmallLoadingAnimation = true)
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
            model.selectedItem = item
            view.showItemDialog()
        }
    }

    override fun refreshItemView() {
        val item = model.selectedItem
        if (item != null) {
            view.closeItemView()
            view.showItemDialog()
        }
    }

    override fun openMyPublications() {
        if (!model.isLoaded()) {
            Log.e("zotero", "tried to change collection before fully loaded!")
            return
        }
        model.usePersonalLibrary()
        view.setTitle("My Publications")
        val entries = model.getMyPublications().sort().map{ListEntry(it)}
        model.isDisplayingItems = entries.isNotEmpty()
        model.setCurrentCollection("zooforzotero_my_publications")
        libraryListViewModel.setItems(entries)
    }

    override fun openTrash() {
        if (!model.isLoaded()) {
            Log.e("zotero", "tried to change collection before fully loaded!")
            return
        }
        model.usePersonalLibrary()
        view.setTitle("Trash")
        val entries = model.getTrashedItems().sort().map{ListEntry(it)}
        model.isDisplayingItems = entries.isNotEmpty()
        model.setCurrentCollection("zooforzotero_Trash")
        libraryListViewModel.setItems(entries)
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

    override fun setCollection(collectionKey: String, fromNavigationDrawer: Boolean) {
        /*SetCollection is the method used to display items on the listView. It
        * has to get the data, then sort it, then provide it to the view.*/
        if (!model.isLoaded()) {
            Log.e("zotero", "tried to change collection before fully loaded!")
            return
        }
        // this check covers if the user has just left their group library from the sidemenu.
        if (fromNavigationDrawer) {
            model.usePersonalLibrary()
        }

        Log.d("zotero", "Got request to change collection to ${collectionKey}")
        model.setCurrentCollection(collectionKey, usePersonalLibrary = fromNavigationDrawer)
        if (collectionKey == "all" && !model.isUsingGroups()) {
            view.setTitle("My Library")
            val entries = model.getLibraryItems().sort().map { ListEntry(it) }
            model.isDisplayingItems = entries.size > 0
            libraryListViewModel.setItems(entries)
        } else if (collectionKey == "unfiled_items") {
            view.setTitle("Unfiled Items")
            val entries = model.getUnfiledItems().sort().map { ListEntry(it) }
            model.isDisplayingItems = entries.size > 0
            libraryListViewModel.setItems(entries)
        } else if (collectionKey == "zooforzotero_Trash"){
            this.openTrash()
        } else if (collectionKey == "zooforzotero_my_publications"){
            this.openMyPublications()
        }else if (collectionKey == "group_all" && model.isUsingGroups()) {
            view.setTitle(model.getCurrentGroup()?.name ?: "ERROR")
            val entries = LinkedList<ListEntry>()
            entries.addAll(model.getCollections().filter {
                !it.hasParent()
            }.sortedBy {
                it.name.toLowerCase(Locale.ROOT)
            }.map { ListEntry(it) })
            entries.addAll(model.getLibraryItems().sort().map { ListEntry(it) })
            model.isDisplayingItems = entries.size > 0
            libraryListViewModel.setItems(entries)
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
            libraryListViewModel.setItems(entries)
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
        libraryListViewModel =
            ViewModelProvider(view as LibraryActivity).get(LibraryListViewModel::class.java)
        libraryLoadingViewModel =
            ViewModelProvider(view as LibraryActivity).get(LibraryLoadingScreenViewModel::class.java)

        view.initUI()

        view.navController.navigate(R.id.libraryLoadingScreen)
        libraryLoadingViewModel.setLoadingMessage("Loading your library")

        Log.d(
            "zotero",
            "On Launch have model.shouldIUpdateLibrary()=${model.shouldIUpdateLibrary()}"
        )
        if (model.isFirstSync()) {
            Log.d("zotero", "First sync - loading library from scratch")
            model.loadGroups()
            this.requestLibraryRefresh()
        } else if (model.shouldIUpdateLibrary()) {
            Log.d("zotero", "Updating library")
            model.loadGroups()
            model.loadLibraryLocally {
                this.requestLibraryRefresh()
            }
        } else {
            Log.d("zotero", "Loading library locally")
            model.loadGroups()
            model.loadLibraryLocally()
        }

        libraryListViewModel.getOnItemClicked().observe(view) { item ->
            this.selectItem(item, longPress = false)
        }

        libraryListViewModel.getOnAttachmentClicked().observe(view) {
            this.openAttachment(it)
        }

        libraryListViewModel.getOnCollectionClicked().observe(view) {
            this.setCollection(it.key)
        }
        libraryListViewModel.getOnLibraryRefreshRequested().observe(view) {
            this.requestLibraryRefresh()
        }
        libraryListViewModel.getScannedBarcode().observe(view) { barcodeNo ->
            view.openZoteroSaveForQuery(barcodeNo)
        }
        libraryListViewModel.getLibraryFilterText().observe(view) {
            Log.d("zotero", "got filter text $it")
            filterEntries(it)
        }
    }

    // extension function to sort lists of items
    private fun List<Item>.sort(): List<Item> {
        if (model.preferences.isSortedAscendingly()) {
            return this.sortedWith(sortMethod)
        }
        return this.sortedWith(sortMethod).reversed()
    }

    fun deleteLocalAttachment(attachment: Item) {
        model.deleteLocalAttachment(attachment)
    }
}
