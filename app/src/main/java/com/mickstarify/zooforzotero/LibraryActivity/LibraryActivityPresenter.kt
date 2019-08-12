package com.mickstarify.zooforzotero.LibraryActivity

import android.content.Context
import android.util.Log
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item

class LibraryActivityPresenter(val view : Contract.View, context : Context) : Contract.Presenter {
    override fun selectItem(item: Item) {
        view.showItemDialog(item)
    }

    override fun setCollection(collectionName: String) {
        Log.d("zotero", "Got request to change collection to ${collectionName}")
        if (collectionName == "all"){
            view.setTitle("My Library")
            view.populateItems(model.getLibraryItems().sortedBy { it.getTitle().toLowerCase() })
        }
        else{
            view.setTitle(collectionName)
            view.populateItems(model.getItemsFromCollection(collectionName).sortedBy { it.getTitle().toLowerCase() })
        }
    }

    override fun testConnection() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun receiveCollections(collections: List<Collection>) {
        for (collection : Collection in collections.filter {
            !it.hasParent()
        }.sortedBy { it.getName().toLowerCase() }){
            Log.d("zotero", "Got collection ${collection.getName()}")
            view.addNavigationEntry(collection, "Catalog")
        }
    }

    override fun createErrorAlert(title: String, message: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemEntries(): List<Contract.View> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val model = LibraryActivityModel(this, context)

    init {
        view.initUI()
        model.requestCollections()
        model.requestItems()
    }
}