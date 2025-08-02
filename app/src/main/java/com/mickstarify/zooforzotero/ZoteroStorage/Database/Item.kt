package com.mickstarify.zooforzotero.ZoteroStorage.Database

import android.os.Parcelable
import android.text.Html
import android.util.Log
import androidx.room.*
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.parcelize.Parcelize
import java.util.LinkedList

/*this code is currently inactive. Just part of my project to migrate to sQL rather than using
* json files for storage. */

@Entity(
    tableName = "ItemInfo", primaryKeys = ["itemKey", "group"],
    indices = arrayOf(Index(value = ["itemKey"], unique = true))
)
@Parcelize
class ItemInfo(
    @ColumnInfo(name = "itemKey") val itemKey: String,
    @ColumnInfo(name = "group") val groupParent: Int = Collection.NO_GROUP_ID,
    @ColumnInfo(name = "version") val version: Int,
    @ColumnInfo(name = "deleted") val deleted: Boolean
) : Parcelable {

    //@Relation(entity = ItemData::class, parentColumn = "itemKey", entityColumn = "parent")
    //lateinit var itemData: List<ItemData>
}
@Parcelize
class Item : Parcelable {
    companion object {
        val ATTACHMENT_TYPE = "attachment"
    }

    @Embedded
    lateinit var itemInfo: ItemInfo

    @Relation(entity = ItemData::class, parentColumn = "itemKey", entityColumn = "parent")
    lateinit var itemData: List<ItemData>

    @Relation(entity = Creator::class, parentColumn = "itemKey", entityColumn = "parent")
    lateinit var creators: List<Creator>

    @Relation(entity = ItemTag::class, parentColumn = "itemKey", entityColumn = "parent")
    lateinit var tags: List<ItemTag>

    @Relation(
        entity = ItemCollection::class,
        parentColumn = "itemKey",
        entityColumn = "itemKey",
        projection = arrayOf("collectionKey")
    )
    lateinit var collections: List<String>

    @Ignore
    var attachments = LinkedList<Item>()

    @Ignore
    var notes = LinkedList<Note>()

    fun getGroup(): Int {
        return itemInfo.groupParent
    }

    fun getSortedCreators(): List<Creator> {
        return this.creators.sortedBy { it.order }
    }

    fun getVersion(): Int {
        return itemInfo.version
    }

    fun getItemData(key: String): String? {
        for (itemData in itemData) {
            if (itemData.name == key) {
                return itemData.value
            }
        }
        Log.d("zotero", "ItemData with key $key not found in Item .")
        return null
    }

    val itemKey: String
        get() = itemInfo.itemKey

    val itemType: String
        get() = data["itemType"] ?: "error"

    @Ignore
    var mappedData: MutableMap<String, String>? = null

    @delegate:Ignore
    val data: Map<String, String> by lazy {
        if (mappedData == null) {
            mappedData = HashMap()
            for (iData in itemData) {
                mappedData!![iData.name] = iData.value
            }
        }
        mappedData!!
    }

    private fun stripHtml(html: String): String {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            return Html.fromHtml(html).toString()
        }
    }

    fun getTitle(): String {
        val title = when (itemType) {
            "case" -> this.data["caseName"]
            "statute" -> this.data["nameOfAct"]
            "note" -> {
                var noteHtml = this.data["note"]
                stripHtml(noteHtml ?: "unknown")
            }
            else -> this.data["title"]
        }

        return (title ?: "unknown")
    }

    fun getAuthor(): String {
        return when (creators.size) {
            0 -> ""
            1 -> creators[0].lastName
            else -> "${getSortedCreators()[0].lastName} et al."
        }
    }

    fun getSortableDateString(): String {
        val date = getItemData("date") ?: ""
        if (date == "") {
            return "ZZZZ"
        }
        return date
    }

    /* Matches the query text against the metadata stored in item,
* checks to see if we can find the text anywhere. Useful for search. */
    fun query(queryText: String): Boolean {
        val queryUpper = queryText.uppercase()

        return this.itemKey.uppercase().contains(queryUpper) ||
                this.tags.joinToString("_").uppercase().contains(queryUpper) ||
                this.data.values.joinToString("_").uppercase().contains(
                    queryUpper
                ) || this.creators.map {
            it.makeString().uppercase()
        }.joinToString("_").contains(queryUpper)
    }

    fun getSortableDateAddedString(): String {
        return getItemData("dateAdded") ?: "XXXX-XX-XX"
    }

    fun getMd5Key(): String {
        return getItemData("md5") ?: ""
    }

    fun getTagList(): List<String> {
        return tags.map { it.tag }
    }

    fun hasParent(): Boolean {
        return this.data.containsKey("parentItem")
    }

    fun getMtime(): Long {
        if (data.containsKey("mtime")) {
            return data["mtime"]!!.toLong()
        }
        Log.e("zotero", "no mtime available for ${itemKey}")
        return 0L
    }

    fun isDownloadable(): Boolean {
        /*Returns whether should be able to download this attachment and open it*/
        if (this.itemType != "attachment") {
            return false
        }

        if (this.data.containsKey("contentType")) {
            return (this.getFileExtension() != "UNKNOWN")
        } else {
            // i don't know, bit of a hack. - basically is returning if attachment and if has extension.
            return (this.getFileExtension() != "UNKNOWN")
        }
        return false
    }

    fun getFileExtension(): String {
        val extension = when (this.data["contentType"]) {
            "application/pdf" -> "pdf"
            "image/vnd.djvu" -> "djvu"
            "application/epub+zip" -> "epub"
            "application/x-mobipocket-ebook" -> "mobi"
            "application/vnd.amazon.ebook" -> "azw"
            "application/vnd.ms-excel" -> "xlsx"
            else -> "UNKNOWN"
        }

        // I probably should have just used file extensions from the beginning...
        if (extension == "UNKNOWN") {
            val filename = if (this.data.containsKey("filename")) {
                this.data["filename"]
            } else {
                this.data["title"]
            }
            return filename?.split(".")?.last() ?: "UNKNOWN"
        }
        return extension
    }

    fun getPdfAttachment(): Item? {
        /*
        * If the item has a PDF attachment, this will return it.
        * Otherwise it will return null
        * */

        for (item in attachments) {
            if (item.isDownloadable() && item.getFileExtension() == "pdf") {
                return item
            }
        }
        return null
    }
}

@Entity(
    tableName = "ItemData", primaryKeys = ["parent", "name"],
    foreignKeys = [ForeignKey(
        entity = ItemInfo::class,
        parentColumns = ["itemKey"],
        childColumns = ["parent"],
        onDelete = ForeignKey.CASCADE
    )]
)
class ItemData(
    @ColumnInfo(name = "parent") val parent: String, //itemKey of parent
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "valueType") val valueType: String
)

@Entity(
    tableName = "ItemCreator", primaryKeys = ["parent", "firstName", "lastName"],
    foreignKeys = [ForeignKey(
        entity = ItemInfo::class,
        parentColumns = ["itemKey"],
        childColumns = ["parent"],
        onDelete = ForeignKey.CASCADE
    )]
)

@Parcelize
class Creator(
    @ColumnInfo(name = "parent") val parent: String, //itemKey of parent
    @ColumnInfo(name = "firstName") val firstName: String,
    @ColumnInfo(name = "lastName") val lastName: String,
    @ColumnInfo(name = "creatorType") val creatorType: String,
    @ColumnInfo(name = "order") val order: Int
) : Parcelable {
    fun makeString(): String {
        return "${firstName} ${lastName}"
    }
}

@Entity(
    tableName = "ItemTags", primaryKeys = ["parent", "tag"],
    foreignKeys = [ForeignKey(
        entity = ItemInfo::class,
        parentColumns = ["itemKey"],
        childColumns = ["parent"],
        onDelete = ForeignKey.CASCADE
    )]
)
@Parcelize
data class ItemTag(
    @ColumnInfo(name = "parent") val parent: String, //itemKey of parent
    @ColumnInfo(name = "tag") val tag: String
) : Parcelable

@Dao
interface ItemDao {
    @Transaction
    @Query("SELECT * FROM itemInfo")
    fun getAll(): Maybe<List<Item>>

    @Transaction
    @Query("SELECT * FROM itemInfo WHERE `itemKey`=:key LIMIT 1")
    fun getItem(key: String): Single<Item>

    @Transaction
    @Query("SELECT * FROM itemInfo WHERE `group`=:groupID and `deleted`=0")
    fun getItemsForGroup(groupID: Int): Maybe<List<Item>>

    @Transaction
    @Query("SELECT * FROM itemInfo WHERE `group`=${GroupInfo.NO_GROUP_ID} and `deleted`=1")
    fun getTrashedItemsForUser(): Maybe<List<Item>>

    @Query("SELECT parent from ItemTags where `tag`=:tag")
    fun getItemKeysWithTag(tag: String): Maybe<List<String>>

    @Transaction
    @Query("DELETE FROM ItemTags WHERE `parent`=:itemKey")
    fun deleteAllTagsForItemKey(itemKey: String): Completable

    @Transaction
    @Query("UPDATE itemInfo SET deleted=1 WHERE `ItemKey`=:itemKey and `group`=:groupID")
    fun moveToTrash(groupID: Int, itemKey: String): Completable

    @Transaction
    @Query("UPDATE itemInfo SET deleted=0 WHERE `ItemKey`=:itemKey and `group`=:groupID")
    fun restoreFromTrash(groupID: Int, itemKey: String): Completable


    @Query("SELECT COUNT(*) != 0 FROM ItemInfo WHERE `itemKey`=:itemKey and `group`=:groupID")
    fun containsItem(groupID: Int, itemKey: String): Single<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItemInfo(items: ItemInfo): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItemData(itemData: List<ItemData>): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCreators(creators: List<Creator>): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItemCollections(collections: List<ItemCollection>): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTags(tags: List<ItemTag>): Completable


    fun insertItem(
        itemInfo: ItemInfo,
        itemDatas: List<ItemData>,
        creators: List<Creator>,
        collections: List<ItemCollection>,
        tags: List<ItemTag>
    ): Completable {
        return insertItemInfo(itemInfo)
            .andThen(insertItemData(itemDatas))
            .andThen(insertCreators(creators))
            .andThen(insertItemCollections(collections))
            .andThen(insertTags(tags))
    }

//
//    @Update
//    fun updateCollections(vararg item: Item)

    @Delete
    fun delete(item: ItemInfo)

    @Transaction
    @Query("DELETE FROM ItemInfo WHERE `itemKey`=:itemKey")
    fun deleteUsingItemKey(itemKey: String)

    @Query("DELETE FROM iteminfo WHERE `group`=:groupID")
    fun deleteAllForGroup(groupID: Int): Completable

    @Transaction
    @Query("DELETE FROM ItemInfo")
    fun deleteAllItems(): Completable
}