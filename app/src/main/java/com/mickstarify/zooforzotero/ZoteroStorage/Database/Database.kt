package com.mickstarify.zooforzotero.ZoteroStorage.Database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mickstarify.zooforzotero.ZoteroAPI.Model.ItemPOJO
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Action
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = arrayOf(
        GroupInfo::class,
        Collection::class,
        RecentlyOpenedAttachment::class,
        ItemInfo::class,
        ItemData::class,
        Creator::class,
        ItemTag::class,
        ItemCollection::class,
        AttachmentInfo::class
    ),
    version = 6,
    exportSchema = true
)
abstract class ZoteroRoomDatabase : RoomDatabase() {
    abstract fun groupInfoDao(): GroupInfoDao
    abstract fun collectionDao(): CollectionDao
    abstract fun recentlyOpenedAttachmentsDao(): RecentlyOpenedAttachmentDao
    abstract fun itemDao(): ItemDao
    abstract fun AttachmentInfoDao(): AttachmentInfoDao
}

@Singleton
class ZoteroDatabase @Inject constructor(val context: Context) {
    val db = Room.databaseBuilder(
        context.applicationContext,
        ZoteroRoomDatabase::class.java, "zotero"
    )
        .allowMainThreadQueries() // I promise I will use this wisely. So far it's only used for querying tags.
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .addMigrations(MIGRATION_4_5)
        .addMigrations(MIGRATION_5_6).build()

    fun addGroup(group: GroupInfo): Completable {
        return db.groupInfoDao().insertGroupInfos(group)
    }

    fun getGroups(): Maybe<List<GroupInfo>> {
        return db.groupInfoDao().getAll()
    }

    fun getItemsForGroup(groupID: Int): Maybe<List<Item>> {
        return db.itemDao().getItemsForGroup(groupID)
    }

    fun getItemKeysWithTag(tag: String): Maybe<List<String>> {
        return db.itemDao().getItemKeysWithTag(tag)
    }

    fun writeItemPOJOs(
        groupID: Int,
        itemsPOJO: List<ItemPOJO>
    ): Completable {
        Log.d("zotero", "writeItemPOJOs() - writing ${itemsPOJO.size} items for groupID=$groupID")
        var completable: Completable = Completable.complete()
        for (item in itemsPOJO) {
            completable = completable.andThen(writeItem(groupID, item))
        }
        return completable.doOnComplete {
            Log.d(
                "zotero",
                "writeItemPOJOs() - completed writing ${itemsPOJO.size} items for groupID=$groupID"
            )
        }.doOnError { error ->
            Log.e("zotero", "writeItemPOJOs() - error writing items: $error")
        }
    }

    fun deleteItem(itemKey: String): Completable {
        return Completable.fromAction(Action {
            db.itemDao().deleteUsingItemKey(itemKey)
        })
    }

    fun deleteCollection(collectionKey: String): Completable {
        return Completable.fromAction(Action {
            db.collectionDao().deleteUsingKey(collectionKey)
        })
    }

    fun writeItem(
        groupID: Int,
        itemPOJO: ItemPOJO
    ): Completable {
        Log.d("zotero", "writeItem() - writing item ${itemPOJO.ItemKey} for groupID=$groupID")
        val itemInfo =
            ItemInfo(itemPOJO.ItemKey, groupID, itemPOJO.version, Boolean.fromInt(itemPOJO.deleted))
        var itemDatas = LinkedList<ItemData>()
        var itemCreators = LinkedList<Creator>()
        for ((key, value) in itemPOJO.data) {
            val itemData = when (key) {
                else -> ItemData(itemPOJO.ItemKey, key, value, "String")
            }
            itemDatas.add(itemData)
        }
        for ((index, creatorPOJO) in itemPOJO.creators.withIndex()) {
            itemCreators.add(
                Creator(
                    itemPOJO.ItemKey,
                    creatorPOJO.firstName,
                    creatorPOJO.lastName,
                    creatorPOJO.creatorType,
                    index
                )
            )
        }
        val itemTags = itemPOJO.tags.map { ItemTag(itemPOJO.ItemKey, it) }
        val collections = itemPOJO.collections.map { ItemCollection(it, itemPOJO.ItemKey) }

        val insertCompletable = Completable.fromAction {
            Log.d("zotero", "About to call insertItem() for ${itemPOJO.ItemKey}")
        }.andThen(
            db.itemDao().insertItem(itemInfo, itemDatas, itemCreators, collections, itemTags)
        ).doOnComplete {
            Log.d("zotero", "writeItem() completed successfully for ${itemPOJO.ItemKey}")
        }.doOnError { error ->
            Log.e("zotero", "writeItem() failed for ${itemPOJO.ItemKey}: $error")
        }
        return insertCompletable
    }

//    fun writeAttachment(item: Item, md5Key: String, downloadedFrom: String): Completable {
//        val attachment = Attachment(item.itemKey, item.groupParent, md5Key, downloadedFrom)
//        return db.AttachmentInfoDao().insertItem(attachment)
//    }

    fun getCollections(groupID: Int): Maybe<List<Collection>> {
        return db.collectionDao().getCollectionsForGroup(groupID)
    }

    fun writeCollections(collections: List<Collection>): Completable {
        return db.collectionDao().insertAllCollections(collections)
    }

    fun addRecentlyOpenedAttachments(recentlyOpenedAttachment: RecentlyOpenedAttachment): Completable {
        return db.recentlyOpenedAttachmentsDao().insert(recentlyOpenedAttachment)
    }

    fun deleteRecentlyOpenedAttachment(itemKey: String): Completable {
        return db.recentlyOpenedAttachmentsDao().delete(itemKey)
    }

    fun getRecentlyOpenedAttachments(): Maybe<List<RecentlyOpenedAttachment>> {
        return db.recentlyOpenedAttachmentsDao().getAll()
    }

    fun getAttachmentsForGroup(groupID: Int): Maybe<List<AttachmentInfo>> {
        return db.AttachmentInfoDao().getAttachmentsForGroup(groupID)
    }

    fun getItemsFromUserTrash(): Maybe<List<Item>> {
        return db.itemDao().getTrashedItemsForUser()
    }

    fun writeAttachmentInfo(
        attachmentInfo: AttachmentInfo
    ): Completable {
        return db.AttachmentInfoDao().updateAttachment(attachmentInfo)
    }

    fun deleteAllItemsForGroup(groupID: Int): Completable {
        return db.itemDao().deleteAllForGroup(groupID)
    }

    fun containsItem(groupID: Int, itemKey: String): Single<Boolean> {
        return db.itemDao().containsItem(groupID, itemKey)
    }

    fun moveItemToTrash(groupID: Int, itemKey: String): Completable {
        return db.itemDao().moveToTrash(groupID, itemKey)
    }

    fun restoreItemFromTrash(groupID: Int, itemKey: String): Completable {
        return db.itemDao().restoreFromTrash(groupID, itemKey)
    }

    fun deleteAllItems(): Completable {
        return db.itemDao().deleteAllItems()
    }

    fun deleteEverything() {
        //todo implement.
    }
}

private fun Boolean.Companion.fromInt(param: Int): Boolean {
    return param != 0
}
