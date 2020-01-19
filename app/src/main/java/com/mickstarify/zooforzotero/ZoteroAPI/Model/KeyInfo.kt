package com.mickstarify.zooforzotero.ZoteroAPI.Model

import com.google.gson.annotations.SerializedName

data class KeyInfo(
    @SerializedName("key")
    val key: String,
    @SerializedName("userID")
    val userID: Int,
    @SerializedName("username")
    val username: String
)