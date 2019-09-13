package com.mickstarify.zooforzotero.ZoteroAPI

import com.google.gson.JsonArray
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.KeyInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ZoteroAPIService {
    @GET("users/{user}/items")
    fun getItems (
        @Path("user") user : String,
        @Query("start") index: Int
    ) : Call<ResponseBody>

    @GET("keys/{key}")
    fun getKeyInfo (
        @Path("key") key : String
    ) : Call<KeyInfo>

    @GET("users/{user}/collections")
    fun getCollections (
        @Path("user") user: String
    ) : Call<List<Collection>>

    @Streaming
    @GET("users/{user}/items/{itemKey}/file")
    fun getItemFile(
        @Path("user") user : String,
        @Path("itemKey") itemKey : String
    ) : Call<ResponseBody>


    @POST("users/{user}/items")
    fun writeItem(
        @Path("user") user: String,
        @Body json: JsonArray
    ): Call<ResponseBody>
}