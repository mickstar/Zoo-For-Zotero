package com.mickstarify.zooforzotero.ZoteroStorage.Database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "ItemCollection", primaryKeys = ["collectionKey", "itemKey"],
    indices = [Index(value=["itemKey","collectionKey"])],
    foreignKeys = [ForeignKey(
        entity = ItemInfo::class,
        parentColumns = ["itemKey"],
        childColumns = ["itemKey"],
        onDelete = ForeignKey.CASCADE
    )]
)
@Parcelize
data class ItemCollection(
    @ColumnInfo(name = "collectionKey") val collectionKey: String,
    @ColumnInfo(name = "itemKey") val itemKey: String
) : Parcelable

// todo write a method for deleting all items from a collection
// todo rewrite librarymodel library loading.