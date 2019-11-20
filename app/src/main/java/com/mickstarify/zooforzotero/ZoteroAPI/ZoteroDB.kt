package com.mickstarify.zooforzotero.ZoteroAPI

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.ArrayMap
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*
import kotlin.collections.HashMap


class ZoteroDB(val context: Context, val prefix: String = "") {
    var collections: List<Collection>? = null
        set(value) {
            field = value
            this.populateCollectionChildren()
            this.createCollectionItemMap()
        }
    var items: List<Item>? = null
        set(value) {
            field = value
            this.createAttachmentsMap()
            this.createCollectionItemMap()
            this.createNotesMap()
        }

    var attachments: MutableMap<String, MutableList<Item>>? = null
    private var notes: MutableMap<String, MutableList<Note>>? = null
    private var itemsFromCollections: HashMap<String, MutableList<Item>>? = null

    // we are adding this prefix stuff so we can have concurrent zoteroDBs that will allow users to store
    // shared collections.
    val COLLECTIONS_FILENAME = if (prefix == "") {
        "collections.json"
    } else {
        "${prefix}_collections.json"
    }
    val ITEMS_FILENAME = if (prefix == "") {
        "items.json"
    } else {
        "${prefix}_items.json"
    }
    val namespace: String = if (prefix == "") {
        "zoteroDB"
    } else {
        "zoteroDB_${prefix}"
    }
    fun isPopulated(): Boolean {
        return !(collections == null || items == null)
    }

    fun commitItemsToStorage() {
        if (items == null) {
            throw Exception("Error, ZoteroDB not initialized. Cannot Commit to storage.")
        }

        val gson = Gson()

        val itemsOut = OutputStreamWriter(context.openFileOutput(ITEMS_FILENAME, MODE_PRIVATE))
        try {
            itemsOut.write(gson.toJson(items))
            itemsOut.close()
        } catch (exception: OutOfMemoryError) {
            Log.d("zotero", "could not cache items, user has not enough space.")
        }
    }

    fun writeDatabaseUpdatedTimestamp() {
        val editor = context.getSharedPreferences(namespace, MODE_PRIVATE).edit()
        val timestamp = System.currentTimeMillis() //timestamp in milliseconds.
        editor.putLong("lastModified", timestamp)
        editor.apply()
    }

    /* Returns the timestamp in milliseconds of when the database was last updated.*/
    fun getLastModifiedTimestamp(): Long {
        val sp = context.getSharedPreferences(namespace, MODE_PRIVATE)
        val timestamp = sp.getLong("lastModified", 0L)
        return timestamp
    }

    fun commitCollectionsToStorage() {
        if (collections == null) {
            throw Exception("Error, ZoteroDB not initialized. Cannot Commit to storage.")
        }

        val gson = Gson()

        val collectionsOut =
            OutputStreamWriter(context.openFileOutput(COLLECTIONS_FILENAME, MODE_PRIVATE))
        try {
            collectionsOut.write(gson.toJson(collections))
            collectionsOut.close()
        } catch (exception: OutOfMemoryError) {
            Log.d("zotero", "could not cache collections, user has not enough space.")
        }
    }

    fun loadItemsFromStorage() {
        val gson = Gson()
        val typeToken = object : TypeToken<List<Item>>() {}.type
        try {
            val itemsJsonReader = InputStreamReader(context.openFileInput(ITEMS_FILENAME))
            this.items = gson.fromJson(itemsJsonReader, typeToken)
            itemsJsonReader.close()

        } catch (e: Exception) {
            Log.e("zotero", "error loading items from storage, deleting file.")
            this.deleteLocalStorage()
            throw Exception("error loading items")
        }
    }

    fun loadCollectionsFromStorage() {
        try {
            val gson = Gson()
            val typeToken = object : TypeToken<List<Collection>>() {}.type
            val collectionsJsonReader =
                InputStreamReader(context.openFileInput(COLLECTIONS_FILENAME))
            this.collections = gson.fromJson(collectionsJsonReader, typeToken)
            collectionsJsonReader.close()
        } catch (e: Exception) {
            Log.e("zotero", "error loading collections from storage, deleting file.")
            this.deleteLocalStorage()
            throw Exception("error loading collections")
        }
    }

    /* Deletes our cached copy of the library. */
    private fun deleteLocalStorage() {
        val collectionsFile = File(COLLECTIONS_FILENAME)
        val itemsFile = File(ITEMS_FILENAME)
        if (collectionsFile.exists()) {
            collectionsFile.delete()
        }
        if (itemsFile.exists()) {
            itemsFile.delete()
        }
    }


    fun hasStorage(): Boolean {
        val collectionsFile = context.getFileStreamPath(COLLECTIONS_FILENAME)
        val itemsFile = context.getFileStreamPath(ITEMS_FILENAME)
        return (collectionsFile.exists() && itemsFile.exists())
    }

    fun getAttachments(itemID: String): List<Item> {
        if (this.attachments == null) {
            this.createAttachmentsMap()
        }

        return this.attachments?.get(itemID) ?: LinkedList()
    }

    fun createAttachmentsMap() {
        if (!isPopulated()) {
            return
        }
        this.attachments = HashMap()

        for (item in items!!) {
            if (!item.data.containsKey("itemType") && item.data.containsKey("parentItem")) {
                continue
            }

            if ((item.data["itemType"] as String) == "attachment") {
                val parentItem = item.data["parentItem"]
                if (parentItem != null) {
                    if (!this.attachments!!.contains(parentItem)) {
                        this.attachments!![parentItem] = LinkedList()
                    }
                    this.attachments!![item.data["parentItem"]]!!.add(item)
                } else {
                    Log.d("zotero", "attachment ${item.getTitle()} has no parent")
                }
            }
        }
    }

    fun createNotesMap() {
        if (!isPopulated()) {
            return
        }

        this.notes = ArrayMap()
        items?.forEach { item: Item ->
            if (item.getItemType() == "note") {
                try {
                    val note = Note(item)
                    if (notes?.containsKey(note.parent) == false) {
                        notes!![note.parent] = LinkedList<Note>()
                    }
                    notes!![note.parent]!!.add(note)
                } catch (e: ExceptionInInitializerError) {
                    Log.e("zotero", "error loading note ${item.ItemKey} error:${e.message}")
                }
            }
        }
    }

    fun createCollectionItemMap() {
        /*all non-nullable assumptions are valid here*/
        if (!isPopulated()) {
            return
        }
        itemsFromCollections = HashMap()
        for (item: Item in this.items!!) {
            for (collection in item.collections) {
                if (!itemsFromCollections!!.containsKey(collection)) {
                    itemsFromCollections!![collection] = LinkedList<Item>()
                }
                itemsFromCollections!![collection]!!.add(item)
            }
        }
    }

    fun getItemsWithoutCollection(): List<Item> {
        if (this.items == null) {
            Log.e("zotero", "Error, items not loaded yet.")
            return LinkedList()
        }
        return (this.getDisplayableItems().filter {
            it.collections.isEmpty()
        })
    }

    /*we will do O(n^2) because I'm not bothered to create a map for what i presume is a small list.*/
    fun populateCollectionChildren() {
        if (collections == null) {
            throw Exception("called populate collections with no collections!")
        }
        for (collection in collections!!) {
            if (collection.hasParent()) {
                collections?.filter { it.key == collection.getParent() }?.firstOrNull()
                    ?.addSubCollection(collection)
            }
        }
    }

    fun getDisplayableItems(): List<Item> {
        if (items != null) {
            return items!!.filter { it.getItemType() != "attachment" && it.getItemType() != "note" }
        } else {
            Log.e("zotero", "error. got request for getDisplayableItems() before items has loaded.")
            return LinkedList()
        }
    }

    fun getItemsFromCollection(collection: String): List<Item> {
        if (this.itemsFromCollections == null) {
            this.createCollectionItemMap()
        }
        return this.itemsFromCollections?.get(collection) ?: LinkedList<Item>() as List<Item>
    }

    fun setItemsVersion(libraryVersion: Int) {
        val sharedPreferences = context.getSharedPreferences(namespace, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("ItemsLibraryVersion", libraryVersion)
        editor.apply()
    }

    fun getLibraryVersion(): Int {
        val sharedPreferences = context.getSharedPreferences(namespace, MODE_PRIVATE)
        return sharedPreferences.getInt("ItemsLibraryVersion", -1)
    }

    fun clearItemsVersion() {
        try {
            val sharedPreferences =
                context.getSharedPreferences(namespace, MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.remove("ItemsLibraryVersion")
            editor.apply()
        } catch (e: Exception) {
            Log.e(context.packageName, "error clearing items version from local db.")
        }
    }

    fun getCollectionId(collectionName: String): String? {
        return this.collections?.filter { it.getName() == collectionName }?.firstOrNull()?.key
    }

    fun getSubCollectionsFor(collectionKey: String): List<Collection> {
        return collections?.filter { it.key == collectionKey }?.firstOrNull()?.getSubCollections()
            ?: LinkedList()
    }

    fun getNotes(itemKey: String): List<Note> {
        if (notes?.containsKey(itemKey) == true) {
            return notes!![itemKey] as List<Note>
        }
        return LinkedList()
    }

    fun deleteItem(key: String) {
        val newItems = LinkedList<Item>()
        items?.forEach {
            if (it.ItemKey != key) {
                newItems.add(it)
            }
        }
        items = newItems
        this.commitItemsToStorage()
    }

    fun applyChangesToItems(modifiedItems: List<Item>) {
        if (items == null) {
            Log.e("zotero", "error items cannot be null!")
            return
        }
        val toAdd: MutableList<Item> = LinkedList(modifiedItems)
        Log.d("zotero", "got a list of modifications of size ${modifiedItems.size}")
        val newItems = LinkedList<Item>()
        for (item in this.items!!) {
            var added = false
            for (modifiedItem: Item in toAdd) {
                if (item.ItemKey == modifiedItem.ItemKey) {
                    newItems.add(modifiedItem)
                    toAdd.remove(modifiedItem)
                    added = true
                    break
                }
            }
            if (!added) {
                newItems.add(item)
            }
        }
        newItems.addAll(toAdd)
        this.items = newItems
        this.commitItemsToStorage()
    }
}