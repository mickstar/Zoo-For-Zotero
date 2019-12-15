package com.mickstarify.zooforzotero.ZoteroStorage.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.reactivex.Completable
import io.reactivex.Maybe

@Database(
    entities = arrayOf(GroupInfo::class, Collection::class, RecentlyOpenedAttachment::class),
    version = 2,
    exportSchema = true
)
abstract class ZoteroRoomDatabase : RoomDatabase() {
    abstract fun groupInfoDao(): GroupInfoDao
    abstract fun collectionDao(): CollectionDao
    abstract fun recentlyOpenedAttachmentsDao(): RecentlyOpenedAttachmentDao
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