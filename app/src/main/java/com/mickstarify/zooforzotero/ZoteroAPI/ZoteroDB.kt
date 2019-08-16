package com.mickstarify.zooforzotero.ZoteroAPI

import android.content.Context
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import java.lang.Exception
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*
import kotlin.collections.HashMap


class ZoteroDB (val context : Context){
    private var itemsFromCollections: HashMap<String, MutableList<Item>>? = null
    var collections : List<Collection>? = null
    var items : List<Item>? = null

    var attachments : MutableMap<String, MutableList<Item>>? = null

    val COLLECTIONS_FILENAME = "collections.json"
    val ITEMS_FILENAME = "items.json"

    fun isPopulated() : Boolean {
        return !(collections == null || items == null)
    }

    fun commitItemsToStorage() {
        if (items == null){
            throw Exception("Error, ZoteroDB not initialized. Cannot Commit to storage.")
        }

        val gson = Gson()

        val itemsOut = OutputStreamWriter(context.openFileOutput(ITEMS_FILENAME, MODE_PRIVATE))
        itemsOut.write(gson.toJson(items))
        itemsOut.close()
    }

    fun commitCollectionsToStorage() {
        if (collections == null){
            throw Exception("Error, ZoteroDB not initialized. Cannot Commit to storage.")
        }

        val gson = Gson()

        val collectionsOut = OutputStreamWriter(context.openFileOutput(COLLECTIONS_FILENAME, MODE_PRIVATE))
        collectionsOut.write(gson.toJson(collections))
        collectionsOut.close()
    }

    fun loadItemsFromStorage(){
        val gson = Gson()
        val typeToken = object : TypeToken<List<Item>>() {}.type
        val itemsJsonReader = InputStreamReader(context.openFileInput(ITEMS_FILENAME))
        this.items = gson.fromJson(itemsJsonReader, typeToken)
        itemsJsonReader.close()
        this.createAttachmentsMap()
        this.createCollectionItemMap()
    }

    fun loadCollectionsFromStorage(){
        val gson = Gson()
        val typeToken = object : TypeToken<List<Collection>>() {}.type
        val collectionsJsonReader = InputStreamReader(context.openFileInput(COLLECTIONS_FILENAME))
        this.collections = gson.fromJson(collectionsJsonReader, typeToken)
        collectionsJsonReader.close()
        this.createCollectionItemMap()
    }

    fun hasStorage() : Boolean {
        val collectionsFile = File(COLLECTIONS_FILENAME)
        val itemsFile = File(ITEMS_FILENAME)
        return (collectionsFile.exists() && itemsFile.exists())
    }

    fun getAttachments (itemID : String) : List<Item> {
        if (this.attachments == null){
            this.createAttachmentsMap()
        }

        return this.attachments?.get(itemID)?:LinkedList()
    }

    fun createAttachmentsMap (){
        if (!isPopulated()){
            return
        }
        this.attachments = HashMap()

        for (item in items!!){
            if (!item.data.containsKey("itemType") && item.data.containsKey("parentItem")){
                continue
            }

            if ((item.data["itemType"] as String) == "attachment"){
                val parentItem = item.data["parentItem"]
                if (parentItem != null) {
                    if (!this.attachments!!.contains(parentItem)) {
                        this.attachments!![parentItem] = LinkedList()
                    }
                    this.attachments!![item.data["parentItem"]]!!.add(item)
                }
                else {
                    Log.d("zotero", "attachment ${item.getTitle()} has no parent")
                }
            }
        }
    }

    fun createCollectionItemMap(){
        /*all non-nullable assumptions are valid here*/
        if (!isPopulated()){
            return
        }
        itemsFromCollections = HashMap()
        for (item : Item in this.items!!){
            for (collection in item.collections){
                if (!itemsFromCollections!!.containsKey(collection)){
                    itemsFromCollections!![collection] = LinkedList<Item>()
                }
                itemsFromCollections!![collection]!!.add(item)
            }
        }
    }

    private fun initDatabase(){
        if (this.isPopulated()) {
            this.createAttachmentsMap()
            this.createCollectionItemMap()
        }
    }

    fun getDisplayableItems(): List<Item> {
        return items!!.filter { it.getItemType() != "attachment" && it.getItemType() != "note"}
    }

    fun getItemsFromCollection(collection: String): List<Item> {
        if (this.itemsFromCollections == null){
            this.createCollectionItemMap()
        }
        return this.itemsFromCollections!![collection]?:LinkedList<Item>() as List<Item>
    }

    fun setItemsVersion(libraryVersion: Int) {
        val sharedPreferences = context.getSharedPreferences("zoteroDB", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("ItemsLibraryVersion", libraryVersion)
        editor.apply()
    }

    fun getItemsVersion() : Int {
        val sharedPreferences = context.getSharedPreferences("zoteroDB", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("ItemsLibraryVersion", -1)
    }

    fun getCollectionId(collectionName: String): String? {
        return this.collections?.filter { it.getName() == collectionName }?.getOrNull(0)?.key
    }
}