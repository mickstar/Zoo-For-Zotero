package com.mickstarify.zooforzotero.LibraryActivity

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item

interface Contract {
    interface View {
        fun initUI ()
        fun createErrorAlert(title : String,message : String)
        fun showLoadingAnimation()
        fun hideLoadingAnimation()
        fun setTitle(title : String)
        fun setSidebarEntries(entries : List<String>)
        fun addNavigationEntry(collection: Collection, parent: String)
        fun populateItems (items : List<Item>)
        fun showItemDialog(item: Item)
    }

    interface Presenter{
        fun createErrorAlert(title : String,message : String)
        fun getItemEntries() : List<View>
        fun testConnection()
        fun receiveCollections(collections : List<Collection>)
        fun setCollection(collectionName: String)
        fun selectItem(item : Item)
        fun requestLibraryRefresh()
        fun stopLoading()
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
    }
}