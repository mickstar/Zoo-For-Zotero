package com.mickstarify.zooforzotero.ZoteroAPI

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.GroupPojo
import com.mickstarify.zooforzotero.ZoteroAPI.Model.KeyInfo
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ZoteroAPIService {
    @GET("users/{user}/items")
    fun getItems(
        @Path("user") user: String,
        @Query("start") index: Int
    ): Observable<Response<ResponseBody>>

    /* Gets items since last update. */
    @GET("users/{user}/items")
    fun getItemsSince(
        @Path("user") user: String,
        @Query("since") modificationSinceVersion: Int,
        @Query("start") index: Int
    ): Observable<Response<ResponseBody>>


    @GET("users/{user}/groups")
    fun getGroupInfo(
        @Path("user") userID: String
    ): Observable<List<GroupPojo>>

    // get items for the group ID
    @GET("groups/{groupID}/items")
    fun getItemsForGroup(
        @Path("groupID") groupID: Int,
        @Query("start") index: Int
    ): Observable<Response<ResponseBody>>

    // get items for the group ID
    @GET("groups/{groupID}/items")
    fun getItemsForGroupSince(
        @Path("groupID") groupID: Int,
        @Query("since") modificationSinceVersion: Int,
        @Query("start") index: Int
    ): Observable<Response<ResponseBody>>

    @GET("groups/{groupID}/collections")
    fun getCollectionsForGroup(
        @Path("groupID") groupID: Int,
        @Query("start") index: Int
    ): Observable<Response<List<Collection>>>

    @Streaming
    @GET("groups/{groupID}/items/{itemKey}/file")
    fun getAttachmentFileFromGroup(
        @Path("groupID") user: Int,
        @Path("itemKey") itemKey: String
    ): Call<ResponseBody>

    @GET("keys/{key}")
    fun getKeyInfo(
        @Path("key") key: String
    ): Call<KeyInfo>

    @GET("users/{user}/collections")
    fun getCollections(
        @Path("user") user: String,
        @Query("start") index: Int
    ): Observable<Response<List<Collection>>>

    @Streaming
    @GET("users/{user}/items/{itemKey}/file")
    fun getItemFile(
        @Path("user") user: String,
        @Path("itemKey") itemKey: String
    ): Call<ResponseBody>


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

    @POST("users/{user}/items/{itemKey}/file")
    fun uploadAttachmentAuthorization(
        @Path("user") user: String,
        @Path("itemKey") itemKey: String,
        @Query("md5") md5: String,
        @Query("filename") filename: String,
        @Query("filesize") filesize: Int,
        @Query("mtime") mtime: Long
    ): Observable<Response<ResponseBody>>

    @POST("users/{user}/items/{itemKey}/file")
    fun uploadAttachment(
        @Path("user") user: String,
        @Path("itemKey") itemKey: String,
        @Query("upload") uploadKey: String
    )
}