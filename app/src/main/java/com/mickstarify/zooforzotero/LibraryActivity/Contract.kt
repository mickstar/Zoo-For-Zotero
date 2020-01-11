package com.mickstarify.zooforzotero.LibraryActivity

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item

interface Contract {
    interface View {
        fun initUI()
        fun createErrorAlert(title: String, message: String, onClick: () -> Unit)
        fun showLoadingAnimation(showScreen: Boolean)
        fun updateLibraryLoadingProgress(
            progress: Int,
            total: Int = -1,
            message: String
        )
        fun addSharedCollection(groupInfo: GroupInfo)
        fun hideLoadingAnimation()
        fun setTitle(title: String)
        fun addNavigationEntry(collection: Collection, parent: String)
        fun populateEntries(entries: List<ListEntry>)
        fun showItemDialog(item: Item, attachments: List<Item>, notes: List<Note>)
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

        fun showBasicSyncAnimation()
        fun hideBasicSyncAnimation()
    }

    interface Presenter {
        fun createErrorAlert(title: String, message: String, onClick: () -> Unit)
        fun receiveCollections(collections: List<Collection>)
        fun setCollection(collectionName: String, isSubCollection: Boolean = false)
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
        fun isShowingContent(): Boolean
        fun cancelAttachmentDownload()
        fun redisplayItems()
        fun createNote(note: Note)
        fun modifyNote(note: Note)
        fun deleteNote(note: Note)
        fun refreshItemView()
        fun displayGroupsOnActionBar(groups: List<GroupInfo>)
        fun openGroup(groupTitle: String)
        fun startUploadingAttachment(attachment: Item)
        fun stopUploadingAttachment()
        fun askToUploadAttachments(changedAttachments: List<Item>)
        fun onResume()
        fun createYesNoPrompt(
            title: String, message: String, yesText: String, noText: String, onYesClick: () -> Unit,
            onNoClick: () -> Unit
        )

        fun showBasicSyncAnimation()
        fun hideBasicSyncAnimation()
    }

    interface Model {
        fun downloadLibrary(refresh: Boolean = false, useSmallLoadingAnimation: Boolean = false)
        fun getLibraryItems(): List<Item>
        fun getItemsFromCollection(collectionName: String): List<Item>
        fun refreshLibrary(useSmallLoadingAnimation: Boolean = false)
        fun getCollections(): List<Collection>
        fun getAttachments(itemKey: String): List<Item>
        fun downloadAttachment(item: Item)
        fun cancelAttachmentDownload()
        fun getSubCollections(collectionName: String): List<Collection>
        fun filterCollections(query: String): List<Collection>
        fun filterItems(query: String): List<Item>
        fun isLoaded(): Boolean
        fun getNotes(itemKey: String): List<Note>
        fun createNote(note: Note)
        fun modifyNote(note: Note)
        fun deleteNote(note: Note)
        fun openPDF(attachment: Item)
        fun openAttachment(item: Item)
        fun deleteAttachment(item: Item)
        fun uploadAttachment(attachment: Item)
        fun getUnfiledItems(): List<Item>
        fun loadGroup(group: GroupInfo, refresh: Boolean = false)
        fun usePersonalLibrary()
        fun getGroupByTitle(groupTitle: String): GroupInfo?
        fun removeFromRecentlyViewed(attachment: Item)
    }
}