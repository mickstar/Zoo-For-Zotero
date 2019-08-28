package com.mickstarify.zooforzotero.LibraryActivity

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import java.io.File

interface Contract {
    interface View {
        fun initUI ()
        fun createErrorAlert(title: String, message: String, onClick: () -> Unit)
        fun showLoadingAnimation()
        fun hideLoadingAnimation()
        fun setTitle(title : String)
        fun addNavigationEntry(collection: Collection, parent: String)
        fun populateEntries(entries: List<ListEntry>)
        fun showItemDialog(item: Item, attachments : List<Item>)
        fun openPDF(attachment: File)
        fun showDownloadProgress()
        fun hideDownloadProgress()
        fun makeToastAlert(message: String)
    }

    interface Presenter{
        fun createErrorAlert(title: String, message: String, onClick: () -> Unit)
        fun testConnection()
        fun receiveCollections(collections : List<Collection>)
        fun setCollection(collectionName: String)
        fun selectItem(item : Item)
        fun requestLibraryRefresh()
        fun stopLoading()
        fun openAttachment(item: Item)
        fun openPDF(attachment: File)
        fun makeToastAlert(message: String)
        fun attachmentDownloadError()
        fun filterEntries(query: String)
        fun closeQuery()
    }

    interface Model {
        fun requestTestConnection()
        fun requestItems(onFinish : () -> (Unit))
        fun requestCollections(onFinish : () -> (Unit))
        fun requestItemsForCollection(collectionKey : String)
        fun requestItem()
        fun getLibraryItems() : List<Item>
        fun getItemsFromCollection(collectionName : String) : List<Item>
        fun refreshLibrary()
        fun getCollections(): List<Collection>?
        fun getAttachments(itemKey: String): List<Item>
        fun openAttachment(item: Item)
        fun getSubCollections(collectionName: String): List<Collection>
        fun filterCollections(query: String): List<Collection>
        fun filterItems(query: String): List<Item>
    }
}