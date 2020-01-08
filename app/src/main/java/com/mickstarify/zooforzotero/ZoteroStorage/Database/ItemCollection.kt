package com.mickstarify.zooforzotero.ZoteroStorage.Database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import kotlinx.android.parcel.Parcelize

@Entity(
    tableName = "ItemCollection", primaryKeys = ["collectionKey", "itemKey"],
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