package com.mickstarify.zooforzotero.ZoteroAPI

import com.google.gson.JsonArray
import com.google.gson.JsonObject
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

    /* Gets items since last update. */
    @GET("users/{user}/items")
    fun getItemsSince(
        @Path("user") user: String,
        @Query("since") modificationSinceVersion: Int,
        @Query("start") index: Int
    ): Call<ResponseBody>

    @GET("keys/{key}")
    fun getKeyInfo (
        @Path("key") key : String
    ) : Call<KeyInfo>

    @GET("users/{user}/collections")
    fun getCollections (
        @Path("user") user: String,
        @Query("start") index: Int
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

    @PATCH("users/{user}/items/{itemKey}")
    fun editNote(
        @Path("user") user: String,
        @Path("itemKey") itemKey: String,
        @Body json: JsonObject
    ): Call<ResponseBody>

    @DELETE("users/{user}/items/{itemKey}")
    fun deleteItem(
        @Path("user") user: String,
        @Path("itemKey") itemKey: String
    ): Call<ResponseBody>
}