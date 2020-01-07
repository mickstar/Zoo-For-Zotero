package com.mickstarify.zooforzotero.ZoteroStorage.Database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "ItemCollection", primaryKeys = ["collectionKey", "itemKey"],
    foreignKeys = [ForeignKey(
        entity = ItemInfo::class,
        parentColumns = ["itemKey"],
        childColumns = ["itemKey"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class itemCollection(
    @ColumnInfo(name = "collectionKey") val collectionKey: String,
    @ColumnInfo(name = "itemKey") val itemKey: String
)