package com.mickstarify.zooforzotero.ZoteroAPI.Model

import com.google.gson.annotations.SerializedName

data class GroupPojo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("version")
    val version: Int,
    @SerializedName("data")
    val groupData: GroupData
)

data class GroupData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("owner")
    val owner: Int,
    @SerializedName("url")
    val url: String,
    @SerializedName("libraryEditing")
    val libraryEditing: String,
    @SerializedName("libraryReading")
    val libraryReading: String,
    @SerializedName("fileEditing")
    val fileEditing: String
)