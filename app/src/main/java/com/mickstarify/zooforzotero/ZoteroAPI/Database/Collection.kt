package com.mickstarify.zooforzotero.ZoteroAPI.Database

import androidx.room.*
import com.mickstarify.zooforzotero.ZoteroAPI.Model.CollectionPOJO
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.*

@Entity(tableName = "Collections")
class Collection(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "version") val version: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "parentCollection") val parentCollection: String,
    @ColumnInfo(name = "group") val groupParent: Int = NO_GROUP_ID
) {

    constructor(collectionPOJO: CollectionPOJO, groupID: Int) : this(
        collectionPOJO.key,
        collectionPOJO.version,
        collectionPOJO.getName(),
        collectionPOJO.getParent(),
        groupID
    ) {
    }

    @Ignore
    private var subCollections: MutableList<Collection>? = LinkedList()

    fun hasParent(): Boolean {
        return parentCollection != "false"
    }

    fun getParent(): String {
        return parentCollection
    }

    fun hasChildren(): Boolean {
        return subCollections?.isEmpty() ?: false
    }

    fun addSubCollection(collection: Collection) {
        if (this.subCollections == null) {
            subCollections = LinkedList()
        }
        // check so we don't add duplicate collections.
        if (this.subCollections?.filter { it.key == collection.key }?.isEmpty() == true) {
            subCollections?.add(collection)
        }
    }

    fun getSubCollections(): List<Collection> {
        return this.subCollections ?: LinkedList()
    }

    companion object {
        const val NO_GROUP_ID = -1
    }
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM Collections")
    fun getAll(): Maybe<List<Collection>>

    @Query("SELECT COUNT(*) FROM Collections")
    fun getNumber(): Int

    @Query("SELECT * FROM Collections WHERE `key`=:key LIMIT 1")
    fun getCollection(key: String): Single<Collection>

    @Query("SELECT * FROM Collections WHERE `group`=:groupID ORDER BY name")
    fun getCollectionsForGroup(groupID: Int): Maybe<List<Collection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllCollections(collections: List<Collection>): Completable

    @Update
    fun updateCollections(vararg collection: Collection)

    @Delete
    fun delete(collection: Collection)
}