package com.mickstarify.zooforzotero.ZoteroStorage.Database

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

@Entity(tableName = "GroupInfo")
data class GroupInfo(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "version") val version: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "libraryEditing") val libraryEditing: String,
    @ColumnInfo(name = "libraryReading") val libraryReading: String,
    @ColumnInfo(name = "fileEditing") val fileEditing: String,
    @ColumnInfo(name = "owner") val owner: Int
)

@Dao
interface GroupInfoDao {
    @Query("SELECT * FROM GroupInfo")
    fun getAll(): Maybe<List<GroupInfo>>

    @Query("SELECT COUNT(*) FROM GroupInfo")
    fun getNumber(): Int

    @Query("SELECT * FROM GroupInfo WHERE id=:id LIMIT 1")
    fun getGroupInfo(id: String): Single<GroupInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGroupInfos(vararg groupInfo: GroupInfo): Completable

    @Update
    fun updateGroupInfos(vararg groupInfo: GroupInfo)

    @Delete
    fun delete(groupInfo: GroupInfo)
}