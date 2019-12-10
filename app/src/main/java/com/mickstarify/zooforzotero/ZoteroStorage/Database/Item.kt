package com.mickstarify.zooforzotero.ZoteroStorage.Database

import android.util.ArrayMap
import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

/*this code is currently inactive. Just part of my project to migrate to sQL rather than using
* json files for storage. */

@Entity(tableName = "Items")
class Item(
    @PrimaryKey val itemKey: String,
    @ColumnInfo(name = "group") val groupParent: Int = Collection.NO_GROUP_ID,
    @ColumnInfo(name = "version") val version: Int
) {
    @Ignore
    var itemData = ArrayMap<String, String>()

    fun addItemData(fieldName: String, value: String) {
        itemData[fieldName] = value
    }
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM Items")
    fun getAll(): Maybe<List<Item>>

    @Query("SELECT * FROM Items WHERE `key`=:key LIMIT 1")
    fun getItem(key: String): Single<Item>

    @Query("SELECT * FROM Collections WHERE `group`=:groupID ORDER BY name")
    fun getCollectionsForGroup(groupID: Int): Maybe<List<Collection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllCollections(collections: List<Collection>): Completable

    @Update
    fun updateCollections(vararg collection: Collection)

    @Delete
    fun delete(collection: Collection)
}