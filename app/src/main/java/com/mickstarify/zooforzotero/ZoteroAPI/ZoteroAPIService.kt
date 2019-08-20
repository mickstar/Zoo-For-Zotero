package com.mickstarify.zooforzotero.ZoteroAPI

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.KeyInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ZoteroAPIService {
    @GET("users/{user}/items")
    fun getItems (
        @Path("user") user : String,
        @Query("start") index : Int,
        @Query("If-Modified-Since-Version") version : Int
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
}