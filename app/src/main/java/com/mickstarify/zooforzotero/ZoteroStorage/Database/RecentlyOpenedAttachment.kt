package com.mickstarify.zooforzotero.ZoteroStorage.Database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Maybe

@Entity(tableName = "RecentlyOpenedAttachment")
data class RecentlyOpenedAttachment(
    @PrimaryKey val itemKey: String,
    @ColumnInfo(name = "version") val version: Int
)

@Dao
interface RecentlyOpenedAttachmentDao {

    @Query("SELECT * FROM RecentlyOpenedAttachment")
    fun getAll(): Maybe<List<RecentlyOpenedAttachment>>

    @Query("DELETE FROM RecentlyOpenedAttachment WHERE itemKey=:itemKey")
    fun delete(itemKey: String): Completable

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(recentlyOpenedAttachment: RecentlyOpenedAttachment): Completable
}