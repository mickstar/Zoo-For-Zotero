package com.mickstarify.zooforzotero.ZoteroAPI.Model

import com.google.gson.annotations.SerializedName
import java.util.*

class Collection(
    @SerializedName("key")
    val key: String,
    @SerializedName("version")
    val version: Int,
    @SerializedName("data")
    val collectionData: CollectionData
) {
    private var subCollections: MutableList<Collection> = LinkedList()

    fun getName(): String {
        return collectionData.name
    }

    fun hasParent(): Boolean {
        return collectionData.parentCollection != "false"
    }

    fun getParent(): String {
        return collectionData.parentCollection
    }

    fun hasChildren(): Boolean {
        return subCollections.isEmpty()
    }

    fun addSubCollection(collection: Collection) {

        if (subCollections == null) {
            subCollections = LinkedList<Collection>()
        }
        if (!this.subCollections.contains(collection)) {
            subCollections.add(collection)
        }
    }

    fun getSubCollections(): List<Collection> {
        return this.subCollections
    }

}

data class CollectionData(
    @SerializedName("name")
    val name: String,
    @SerializedName("parentCollection")
    val parentCollection: String
) {

}