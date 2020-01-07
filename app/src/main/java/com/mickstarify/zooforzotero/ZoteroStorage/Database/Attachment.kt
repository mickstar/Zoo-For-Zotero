//package com.mickstarify.zooforzotero.ZoteroStorage.Database
//
//import androidx.room.*
//import io.reactivex.Completable
//import io.reactivex.Maybe
//import io.reactivex.Single
//
//
///*
//* We are going to have a attachments table that will store metadata related to attachments.
//* whenever an attachment is downloaded from
//* */
//@Entity(
//    tableName = "Attachments", primaryKeys = ["itemKey", "group"],
//    foreignKeys = [androidx.room.ForeignKey(
//        entity = com.mickstarify.zooforzotero.ZoteroStorage.Database.Item::class,
//        parentColumns = ["itemKey"],
//        childColumns = ["itemKey"],
//        onDelete = androidx.room.ForeignKey.CASCADE
//    )]
//)
//class Attachment(
//    @ColumnInfo(name = "itemKey") val itemKey: String,
//    @ColumnInfo(name = "group") val groupParent: Int = Collection.NO_GROUP_ID,
//    @ColumnInfo(name = "md5Key") val md5Key: String = "",
//    @ColumnInfo(name = "downloadedFrom") val downloadedFrom: String = "UNSET"
//) {
//
//    @Relation(entity = Item::class, parentColumn = "itemKey", entityColumn = "itemKey")
//    lateinit var item: Item
//
//    fun getFilename(): String {
//        return item.itemData.filter { it.name == "filename" }.firstOrNull()?.value ?: "unknown"
//    }
//}
//
//@Dao
//interface AttachmentDao {
//    @Query("SELECT * FROM Attachments")
//    fun getAll(): Maybe<List<Attachment>>
//
//    @Query("SELECT * FROM Attachments WHERE `itemKey`=:key LIMIT 1")
//    fun getAttachment(key: String): Single<Attachment>
//
//    @Query("SELECT * FROM Attachments WHERE `group`=:groupID")
//    fun getAttachmentsForGroup(groupID: Int): Maybe<List<Attachment>>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insertAttachments(items: List<Attachment>): Completable
//
//    @Update
//    fun updateCollections(vararg item: Attachment)
//
//    @Delete
//    fun delete(item: Attachment)
//}