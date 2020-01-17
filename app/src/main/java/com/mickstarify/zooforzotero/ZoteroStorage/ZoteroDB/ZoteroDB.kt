package com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.ArrayMap
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mickstarify.zooforzotero.ZooForZoteroApplication
import com.mickstarify.zooforzotero.ZoteroAPI.Model.ItemPOJO
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.AttachmentInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.CompletableOnSubscribe
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

class ZoteroDB constructor(
    val context: Context,
    val groupID: Int
) {

    init {
        ((context as Activity).application as ZooForZoteroApplication).component.inject(this)
    }

    @Inject
    lateinit var zoteroDatabase: ZoteroDatabase

    val prefix = if (groupID == Collection.NO_GROUP_ID) {
        ""
    } else {
        groupID.toString()
    }

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

    // map that stores attachmentItem classes by ItemKey
    var attachments: MutableMap<String, MutableList<Item>>? = null
    // map that stores attachmentInfo classes by ItemKey.
    // This is used to store metadata related to items that don't go in the item database class
    // such a design was picked to seperate the data that is from the zotero api official server and
    // the metadata i store customly.
    var attachmentInfo: MutableMap<String, AttachmentInfo>? = null
    private var notes: MutableMap<String, MutableList<Note>>? = null
    private var itemsFromCollections: HashMap<String, MutableList<Item>>? = null

    // we are adding this prefix stuff so we can have concurrent zoteroDBs that will allow users to store
    // shared collections.
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
        Log.d("zotero", "committing items to storage")
        commitItemsToStorage2()
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

    fun commitItemsToStorage2() {
        zoteroDatabase.writeItems(groupID, items!!).subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver {
                override fun onComplete() {
                    Log.d("zotero", "finished writing items to database.")
                }

                override fun onSubscribe(d: Disposable) {
                    Log.d("zotero", "started writing items to database.")
                }

                override fun onError(e: Throwable) {
                    Log.e("zotero", "got error ${e}")
                }

            })
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

    fun commitCollectionsToDatabase() {
        if (collections == null) {
            throw Exception("Error, ZoteroDB not initialized. Cannot Commit to storage.")
        }
        zoteroDatabase.writeCollections(this.collections!!).subscribeOn(Schedulers.io()).subscribe()
    }

    fun loadCollectionsFromDatabase(): Completable {
        val observable = zoteroDatabase.getCollections(groupID)

        return Completable.fromMaybe(zoteroDatabase.getCollections(groupID).doOnSuccess(
            Consumer {
                collections = it
            }
        ))
    }

    fun loadItemsFromDatabase(): Completable {
        /* Load the items from Database as well as all the attachments. */
        val completable =
            Completable.fromMaybe(zoteroDatabase.getItemsForGroup(groupID).doOnSuccess(
                Consumer {
                    items = it
                }
            )).andThen(
                Completable.fromMaybe(
                    zoteroDatabase.getAttachmentsForGroup(groupID).doOnSuccess(Consumer {
                        attachmentInfo = HashMap<String, AttachmentInfo>()
                        for (attachment in it) {
                            attachmentInfo!![attachment.itemKey] = attachment
                        }
                    })
                )
            ).andThen(Completable.fromAction {
//                throw Exception("breh")
            })
        return completable
    }

    fun destroyItemsDatabase(): Completable {
        this.clearItemsVersion()
        return zoteroDatabase.deleteAllItemsForGroup(groupID)
    }

    /* We used to store the library as a json file which was a pain in the arse for handling modifications.*/
    fun deleteLegacyStorage() {
        val itemsFile = File(ITEMS_FILENAME)
        if (itemsFile.exists()) {
            itemsFile.delete()
        }
    }

    fun hasLegacyStorage(): Boolean {
        val itemsFile = context.getFileStreamPath(ITEMS_FILENAME)
        return (itemsFile.exists())
    }

    fun getAttachments(itemID: String): List<Item> {
        if (this.attachments == null) {
            this.createAttachmentsMap()
        }

        return this.attachments?.get(itemID) ?: LinkedList()
    }

    fun createAttachmentsMap() {
        this.attachments = HashMap()

        if (items == null){
            Log.e("zotero", "items are null for some reason.")
            this.items = LinkedList()
        }

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
            if (item.itemType == "note") {
                try {
                    val note = Note(item)
                    if (notes?.containsKey(note.parent) == false) {
                        notes!![note.parent] = LinkedList<Note>()
                    }
                    notes!![note.parent]!!.add(note)
                } catch (e: ExceptionInInitializerError) {
                    Log.e("zotero", "error loading note ${item.itemKey} error:${e.message}")
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
            return items!!.filter { !it.hasParent() }
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
        Log.d("zotero", "setting library version ${libraryVersion} on ${namespace}")
        val sharedPreferences = context.getSharedPreferences(namespace, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("ItemsLibraryVersion", libraryVersion)
        editor.apply()
    }

    fun setDownloadProgress(progress: ItemsDownloadProgress) {
        val sharedPreferences = context.getSharedPreferences(namespace, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("downloadedAmount", progress.nDownloaded)
        editor.putInt("total", progress.total)
        editor.putInt("downloadVersion", progress.libraryVersion)
        editor.apply()
    }

    fun destroyDownloadProgress() {
        val sharedPreferences = context.getSharedPreferences(namespace, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("downloadedAmount")
        editor.remove("total")
        editor.remove("downloadVersion")
        editor.commit()
    }

    fun getDownloadProgress(): ItemsDownloadProgress? {
        val sharedPreferences = context.getSharedPreferences(namespace, MODE_PRIVATE)
        val nDownload = sharedPreferences.getInt("downloadedAmount", 0)
        if (nDownload == 0) {
            return null
        }

        val total = sharedPreferences.getInt("total", 0)
        val downloadVersion = sharedPreferences.getInt("downloadVersion", 0)
        if (nDownload == 0 || total == nDownload) {
            // completed job. there is no download progress.
            return null
        }
        return ItemsDownloadProgress(downloadVersion, nDownload, total)
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
        return this.collections?.filter { it.name == collectionName }?.firstOrNull()?.key
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
            if (it.itemKey != key) {
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
                if (item.itemKey == modifiedItem.itemKey) {
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

    fun getItemWithKey(itemKey: String): Item? {
        return items?.filter { it -> it.itemKey == itemKey }?.firstOrNull()
    }

    fun hasMd5Key(item: Item, onlyWebdav: Boolean = false): Boolean {
        return getMd5Key(item, onlyWebdav) != ""
    }

    fun getMd5Key(item: Item, onlyWebdav: Boolean = false): String {
        if (attachmentInfo == null) {
            Log.e("zotero", "error attachment metadata isn't loaded")
            return ""
        }
        val attachmentInfo = this.attachmentInfo!![item.itemKey]
        if (attachmentInfo == null) {
            val md5Key = item.data["md5"]
            if (md5Key != null){
                return md5Key
            }
            Log.d("zotero", "No metadata available for ${item.itemKey}")
            return ""
        }

        return attachmentInfo.md5Key
    }

    fun migrateItemsFromOldStorage(): Completable {
        val gson = Gson()
        val typeToken = object : TypeToken<List<ItemPOJO>>() {}.type
        val itemPojos: List<ItemPOJO>
        try {
            val itemsJsonReader = InputStreamReader(context.openFileInput(ITEMS_FILENAME))
            itemPojos = gson.fromJson(itemsJsonReader, typeToken)
            itemsJsonReader.close()

        } catch (e: Exception) {
            Log.e("zotero", "error loading items from storage, deleting file.")
            this.deleteLegacyStorage()
            throw Exception("error loading items")
        }
        return zoteroDatabase.writeItemPOJOs(groupID, itemPojos).andThen(
            this.loadCollectionsFromDatabase()
        ).andThen(
            this.loadItemsFromDatabase()
        ).andThen(Completable.fromAction {
            val libraryVersion = this.getLibraryVersion()
            this.deleteLegacyStorage()
            this.setItemsVersion(libraryVersion)
        })
    }

    fun scanAndIndexAttachments(attachmentStorageManager: AttachmentStorageManager): io.reactivex.Observable<IndexFilesProgress> {
        /* Checks every attachment in the storage directory and creates metadata for the attachment. */

        var toIndex = LinkedList<Item>()
        Log.d("zotero", "items: ${items?.size} attachments: ${attachments?.size}")
        for ((itemKey: String, items: List<Item>) in this.attachments!!) {
            for (item in items) {
                if (this.attachmentInfo!!.containsKey(item.itemKey)) {
                    // we already have this metadata. skipping.
                    continue
                }
                if (attachmentStorageManager.checkIfAttachmentExists(item)) {
                    toIndex.add(item)
                }
            }
        }
        val observable = io.reactivex.Observable.create<IndexFilesProgress> { emitter ->
            var index = 0
            for (item in toIndex) {
                index++
                emitter.onNext(
                    IndexFilesProgress(
                        index,
                        toIndex.size,
                        item.data["filename"] ?: "unknown file"
                    )
                )
                var md5Key = ""
                try {
                    md5Key = attachmentStorageManager.calculateMd5(item)
                } catch (e: Exception) {
                    Log.d("zotero", "error calculating md5 for $item")
                }
                if (md5Key == "") {
                    continue
                }
                val mtime = (item.data["mtime"] ?: "0" as String).toLong()

                this.updateAttachmentMetadata(item.itemKey, md5Key, mtime, AttachmentInfo.LOCALSYNC)
                    .blockingAwait()
            }
            emitter.onComplete()
        }
        return observable
    }

    fun updateAttachmentMetadata(
        itemKey: String,
        md5Key: String,
        mtime: Long,
        downloadedFrom: String = AttachmentInfo.UNSET
    ): Completable {
        val mDownloadedFrom = if (downloadedFrom == AttachmentInfo.UNSET) {
            // check to see if webdav is on, which implies that i was downloaded from webdav.
            if (com.mickstarify.zooforzotero.PreferenceManager(context).isWebDAVEnabled()) {
                AttachmentInfo.WEBDAV
            } else {
                AttachmentInfo.ZOTEROAPI
            }
        } else {
            downloadedFrom
        }
        val attachmentInfo = AttachmentInfo(itemKey, groupID, md5Key, mtime, mDownloadedFrom)
        Log.d("zotero", "adding metadata for ${itemKey}, $md5Key - ${mDownloadedFrom}")

        this.attachmentInfo!![itemKey] = attachmentInfo
        return zoteroDatabase.writeAttachmentInfo(attachmentInfo)
    }
}