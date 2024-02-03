package com.mickstarify.zooforzotero.LibraryActivity

import com.mickstarify.zooforzotero.LibraryActivity.ViewModels.LibraryListViewModel
import com.mickstarify.zooforzotero.LibraryActivity.ViewModels.LibraryLoadingScreenViewModel
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item

interface Contract {
    interface View {
        fun initUI()
        fun createErrorAlert(title: String, message: String, onClick: () -> Unit)

        //        fun showLoadingAnimation(showScreen: Boolean)
        fun updateLibraryLoadingProgress(
            progress: Int,
            total: Int = -1,
            message: String
        )

        fun addSharedCollection(groupInfo: GroupInfo)

        //        fun hideLoadingAnimation()
        fun setTitle(title: String)
        fun addNavigationEntry(collection: Collection, parent: String)
        fun populateEntries(entries: List<ListEntry>)
        fun showItemDialog()
        fun updateAttachmentDownloadProgress(progress: Int, total: Int)
        fun hideAttachmentDownloadProgress()
        fun makeToastAlert(message: String)
        fun showLibraryContentDisplay(message: String = "")
        fun hideLibraryContentDisplay()
        fun clearSidebar()
        fun closeItemView()
        fun showAttachmentUploadProgress(attachment: Item)
        fun hideAttachmentUploadProgress()
        fun createYesNoPrompt(
            title: String, message: String, yesText: String, noText: String, onYesClick: () -> Unit,
            onNoClick: () -> Unit
        )

        fun showNote(note: Note)
        fun highlightMenuItem(state: LibraryModelState)
        fun showLoadingAlertDialog(message: String)
        fun hideLoadingAlertDialog()
    }

    interface Presenter {
        fun createErrorAlert(title: String, message: String, onClick: () -> Unit)
        fun receiveCollections(collections: List<Collection>)
        fun setCollection(collectionName: String, fromNavigationDrawer: Boolean = false)
        fun selectItem(item: Item, longPress: Boolean = false)
        fun requestLibraryRefresh()
        fun showLibraryLoadingAnimation()
        fun hideLibraryLoadingAnimation()
        fun openAttachment(item: Item)
        fun finishDownloadingAttachment()
        fun makeToastAlert(message: String)
        fun attachmentDownloadError(message: String = "")
        fun updateAttachmentDownloadProgress(progress: Long, total: Long)
        fun filterEntries(query: String)
        fun closeQuery()
        fun updateLibraryRefreshProgress(
            progress: Int,
            total: Int,
            message: String
        )

        fun isLiveSearchEnabled(): Boolean
        fun isShowingContent(): Boolean
        fun cancelAttachmentDownload()
        fun redisplayItems()
        fun createNote(note: Note)
        fun modifyNote(note: Note)
        fun deleteNote(note: Note)
        fun refreshItemView()
        fun displayGroupsOnActionBar(groups: List<GroupInfo>)
        fun openGroup(groupTitle: String)
        fun startUploadingAttachmentProgress(attachment: Item)
        fun stopUploadingAttachmentProgress()
        fun onResume()
        fun createYesNoPrompt(
            title: String, message: String, yesText: String, noText: String, onYesClick: () -> Unit,
            onNoClick: () -> Unit
        )

        fun showBasicSyncAnimation()
        fun hideBasicSyncAnimation()
        fun openTrash()
        fun uploadAttachment(item: Item)
        fun requestForceResync()
        fun backButtonPressed()
        fun addFilterState(query: String)
        fun openMyPublications()
        fun onTagOpen(tagName: String)
        fun showLoadingAlertDialog(message: String)
        fun hideLoadingAlertDialog()

        var libraryListViewModel: LibraryListViewModel
        var libraryLoadingViewModel: LibraryLoadingScreenViewModel
    }

    interface Model {
        fun downloadLibrary(doRefresh: Boolean = false, useSmallLoadingAnimation: Boolean = false)
        fun getLibraryItems(): List<Item>
        fun getItemsFromCollection(collectionName: String): List<Item>
        fun refreshLibrary(useSmallLoadingAnimation: Boolean = false)
        fun getCollections(): List<Collection>
        fun getAttachments(itemKey: String): List<Item>
        fun getMyPublications(): List<Item>
        fun downloadAttachment(item: Item)
        fun cancelAttachmentDownload()
        fun getSubCollections(collectionName: String): List<Collection>
        fun filterCollections(query: String): List<Collection>
        fun filterItems(query: String): List<Item>
        fun isLoaded(): Boolean
        fun createNote(note: Note)
        fun modifyNote(note: Note)
        fun deleteNote(note: Note)
        fun openPDF(attachment: Item)
        fun openAttachment(item: Item)
        fun deleteAttachment(item: Item)
        fun uploadAttachment(attachment: Item)
        fun getUnfiledItems(): List<Item>
        fun startGroupSync(group: GroupInfo, refresh: Boolean = false)
        fun usePersonalLibrary()
        fun getGroupByTitle(groupTitle: String): GroupInfo?
        fun removeFromRecentlyViewed(attachment: Item)
        fun loadPriorState()
        fun getItemsForTag(tagName: String): List<Item>
        fun deleteLocalAttachment(attachment: Item)
        fun isFirstSync(): Boolean
    }
}
