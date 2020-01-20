package com.mickstarify.zooforzotero.ZoteroStorage.Database

import android.os.Parcelable
import android.text.Html
import android.util.Log
import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.HashMap

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
    @ColumnInfo(name = "version") val version: Int
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

    fun getGroup(): Int {
        return itemInfo.groupParent
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
        Log.d("zotero", "ItemData with key $key not found in Item.")
        return null
    }

    val itemKey: String
        get() = itemInfo.itemKey

    val itemType: String
        get() = getItemData("itemType") ?: "error"

    @IgnoredOnParcel
    @Ignore
    var mappedData: MutableMap<String, String>? = null

    @IgnoredOnParcel
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
            "case" -> this.getItemData("caseName")
            "statute" -> this.getItemData("nameOfAct")
            "note" -> {
                var noteHtml = this.getItemData("note")
                stripHtml(noteHtml ?: "unknown")
            }
            else -> this.getItemData("title")
        }

        return (title ?: "unknown")
    }

    fun getAuthor(): String {
        return when (creators.size) {
            0 -> ""
            1 -> creators[0].lastName
            else -> "${creators[0].lastName} et al."
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
        val queryUpper = queryText.toUpperCase(Locale.ROOT)

        return this.itemKey.toUpperCase(Locale.ROOT).contains(queryUpper) ||
                this.tags.joinToString("_").toUpperCase(Locale.ROOT).contains(queryUpper) ||
                this.data.values.joinToString("_").toUpperCase(Locale.ROOT).contains(
                    queryUpper
                ) || this.creators.map {
            it.makeString().toUpperCase(Locale.ROOT)
        }.joinToString("_").contains(queryUpper)
    }

    fun getSortableDateAddedString(): String {
        return getItemData("date") ?: "XXXX-XX-XX"
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
        }
        return false
    }

    fun getFileExtension(): String {
        return when (this.data["contentType"]) {
            "application/pdf" -> "pdf"
            "image/vnd.djvu" -> "djvu"
            "application/epub+zip" -> "epub"
            else -> "UNKNOWN"
        }
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
@Parcelize
class ItemData(
    @ColumnInfo(name = "parent") val parent: String, //itemKey of parent
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "valueType") val valueType: String
) : Parcelable

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
    @ColumnInfo(name = "creatorType") val creatorType: String
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
    @Query("SELECT * FROM itemInfo WHERE `group`=:groupID")
    fun getItemsForGroup(groupID: Int): Maybe<List<Item>>

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


    @Transaction
    fun insertItem(
        itemInfo: ItemInfo,
        itemDatas: List<ItemData>,
        creators: List<Creator>,
        collections: List<ItemCollection>,
        tags: List<ItemTag>
    ) {
        insertItemInfo(itemInfo).blockingAwait()
        insertItemData(itemDatas).blockingAwait()
        insertCreators(creators).blockingAwait()
        insertItemCollections(collections).blockingAwait()
        insertTags(tags).blockingAwait()
    }

//
//    @Update
//    fun updateCollections(vararg item: Item)

    @Delete
    fun delete(item: ItemInfo)

    @Query("DELETE FROM iteminfo WHERE `group`=:groupID")
    fun deleteAllForGroup(groupID: Int): Completable
}