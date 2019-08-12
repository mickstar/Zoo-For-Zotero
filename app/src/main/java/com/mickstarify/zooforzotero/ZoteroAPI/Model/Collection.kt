package com.mickstarify.zooforzotero.ZoteroAPI.Model

import com.google.gson.annotations.SerializedName

class Collection (
    @SerializedName("key")
    val key : String,
    @SerializedName("version")
    val version : Int,
    @SerializedName("data")
    val collectionData : CollectionData
) {

    fun getName() : String{
        return collectionData.name
    }

    fun hasParent() : Boolean {
        return collectionData.parentCollection != "false"
    }

    fun getParent() : String {
        return collectionData.parentCollection
    }

}

data class CollectionData (
    @SerializedName("name")
    val name : String,
    @SerializedName("parentCollection")
    val parentCollection: String
) {

}