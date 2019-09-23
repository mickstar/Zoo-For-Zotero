package com.mickstarify.zooforzotero.LibraryActivity

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import java.io.File

interface Contract {
    interface View {
        fun initUI ()
        fun createErrorAlert(title: String, message: String, onClick: () -> Unit)
        fun showLoadingAnimation(showScreen: Boolean)
        fun updateLibraryLoadingProgress(progress: Int, total: Int = -1)
        fun hideLoadingAnimation()
        fun setTitle(title : String)
        fun addNavigationEntry(collection: Collection, parent: String)
        fun populateEntries(entries: List<ListEntry>)
        fun showItemDialog(item: Item, attachments: List<Item>, notes: List<Note>)
        fun updateAttachmentDownloadProgress(progress: Int, total: Int)
        fun hideAttachmentDownloadProgress()
        fun makeToastAlert(message: String)
        fun showLibraryContentDisplay(message: String = "")
        fun hideLibraryContentDisplay()
    }

    interface Presenter{
        fun createErrorAlert(title: String, message: String, onClick: () -> Unit)
        fun testConnection()
        fun receiveCollections(collections : List<Collection>)
        fun setCollection(collectionName: String)
        fun selectItem(item : Item)
        fun requestLibraryRefresh()
        fun stopLoadingLibrary()
        fun openAttachment(item: Item)
        fun openPDF(attachment: File)
        fun makeToastAlert(message: String)
        fun attachmentDownloadError()
        fun updateAttachmentDownloadProgress(progress: Long, total: Long)
        fun filterEntries(query: String)
        fun closeQuery()
        fun updateLibraryRefreshProgress(progress: Int, total: Int)
        fun isShowingContent(): Boolean
        fun cancelAttachmentDownload()
        fun redisplayItems()
        fun createNote(note: Note)
        fun modifyNote(note: Note)
        fun deleteNote(note: Note)
        fun refreshItemView()
    }

    interface Model {
        fun requestItems(onFinish: () -> (Unit), useCaching: Boolean = true)
        fun requestCollections(onFinish: () -> (Unit), useCaching: Boolean = true)
        fun getLibraryItems() : List<Item>
        fun getItemsFromCollection(collectionName : String) : List<Item>
        fun refreshLibrary()
        fun getCollections(): List<Collection>
        fun getAttachments(itemKey: String): List<Item>
        fun downloadAttachment(item: Item)
        fun cancelAttachmentDownload()
        fun getSubCollections(collectionName: String): List<Collection>
        fun filterCollections(query: String): List<Collection>
        fun filterItems(query: String): List<Item>
        fun isLoaded(): Boolean
        fun loadCollectionsLocally(onFinish: () -> Unit)
        fun getNotes(itemKey: String): List<Note>
        fun createNote(note: Note)
        fun modifyNote(note: Note)
        fun deleteNote(note: Note)
        fun openPDF(attachment: File)
    }
}