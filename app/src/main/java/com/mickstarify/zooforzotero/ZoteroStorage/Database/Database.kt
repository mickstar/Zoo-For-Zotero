package com.mickstarify.zooforzotero.ZoteroStorage.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mickstarify.zooforzotero.ZoteroAPI.Model.ItemPOJO
import io.reactivex.Completable
import io.reactivex.Maybe
import java.util.*

@Database(
    entities = arrayOf(
        GroupInfo::class,
        Collection::class,
        RecentlyOpenedAttachment::class,
        ItemInfo::class,
        ItemData::class,
        Creator::class,
        ItemTag::class,
        itemCollection::class
//        Attachment::class
    ),
    version = 3,
    exportSchema = true
)
abstract class ZoteroRoomDatabase : RoomDatabase() {
    abstract fun groupInfoDao(): GroupInfoDao
    abstract fun collectionDao(): CollectionDao
    abstract fun recentlyOpenedAttachmentsDao(): RecentlyOpenedAttachmentDao
    abstract fun itemDao(): ItemDao
//    abstract fun attachmentDao(): AttachmentDao
}

class ZoteroDatabase constructor(val context: Context) {
    val db = Room.databaseBuilder(
        context.applicationContext,
        ZoteroRoomDatabase::class.java, "zotero"
    ).addMigrations(MIGRATION_1_2).build()

    fun addGroup(group: GroupInfo): Completable {
        return db.groupInfoDao().insertGroupInfos(group)
    }

    fun getGroups(): Maybe<List<GroupInfo>> {
        return db.groupInfoDao().getAll()
    }

    fun getItemsForGroup(groupID: Int): Maybe<List<Item>> {
        return db.itemDao().getItemsForGroup(groupID)
    }

    fun writeItemPOJOs(
        groupID: Int,
        itemsPOJO: List<ItemPOJO>
    ): Completable {
        var completable = Completable.complete()
        for (item in itemsPOJO) {
            completable = completable.andThen(writeItem(groupID, item))
        }
        return completable
    }

    fun writeItems(
        groupID: Int,
        items: List<Item>
    ): Completable {
        var completable = Completable.complete()
        for (item in items) {
            completable = completable.andThen(writeItem(groupID, item))
        }
        return completable
    }

    fun writeItem(groupID: Int, item: Item): Completable {
        return Completable.fromAction {
            db.itemDao().insertItem(item.itemInfo, item.itemData, item.creators, item.tags)
        }
    }

    fun writeItem(
        groupID: Int,
        itemPOJO: ItemPOJO
    ): Completable {
        val itemInfo = ItemInfo(itemPOJO.ItemKey, groupID, itemPOJO.version)
        var insertItemCompletable = db.itemDao().insertItemInfo(itemInfo)
        var itemDatas = LinkedList<ItemData>()
        var itemCreators = LinkedList<Creator>()
        for ((key, value) in itemPOJO.data) {
            val itemData = when (key) {
                else -> ItemData(itemPOJO.ItemKey, key, value, "String")
            }
            itemDatas.add(itemData)
        }
        for (creatorPOJO in itemPOJO.creators) {
            itemCreators.add(
                Creator(
                    itemPOJO.ItemKey,
                    creatorPOJO.firstName,
                    creatorPOJO.lastName,
                    creatorPOJO.creatorType
                )
            )
        }
        val itemTags = itemPOJO.tags.map { ItemTag(itemPOJO.ItemKey, it) }
        return Completable.fromAction {
            db.itemDao().insertItem(itemInfo, itemDatas, itemCreators, itemTags)
        }
    }

//    fun writeAttachment(item: Item, md5Key: String, downloadedFrom: String): Completable {
//        val attachment = Attachment(item.itemKey, item.groupParent, md5Key, downloadedFrom)
//        return db.attachmentDao().insertItem(attachment)
//    }

    fun getNumber(): Int {
        return db.groupInfoDao().getNumber()
    }

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
}