package com.mickstarify.zooforzotero.ZoteroAPI

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mickstarify.zooforzotero.ZoteroAPI.Model.CollectionPOJO
import com.mickstarify.zooforzotero.ZoteroAPI.Model.DeletedEntriesPojo
import com.mickstarify.zooforzotero.ZoteroAPI.Model.GroupPojo
import com.mickstarify.zooforzotero.ZoteroAPI.Model.KeyInfo
import io.reactivex.Observable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ZoteroAPIService {
    @GET("users/{user}/items")
    fun getItems(
        @Header("If-Modified-Since-Version") ifModifiedSinceVersion: Int,
        @Path("user") user: String,
        @Query("start") index: Int
    ): Observable<Response<ResponseBody>>

    /* Gets items since last update. */
    @GET("users/{user}/items")
    fun getItemsSince(
        @Header("If-Modified-Since-Version") ifModifiedSinceVersion: Int,
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
        @Header("If-Modified-Since-Version") ifModifiedSinceVersion: Int,
        @Path("groupID") groupID: Int,
        @Query("start") index: Int
    ): Observable<Response<ResponseBody>>

    // get items for the group ID
    @GET("groups/{groupID}/items")
    fun getItemsForGroupSince(
        @Header("If-Modified-Since-Version") ifModifiedSinceVersion: Int,
        @Path("groupID") groupID: Int,
        @Query("since") modificationSinceVersion: Int,
        @Query("start") index: Int
    ): Observable<Response<ResponseBody>>

    @GET("groups/{groupID}/collections")
    fun getCollectionsForGroup(
        @Header("If-Modified-Since-Version") ifModifiedSinceVersion: Int,
        @Path("groupID") groupID: Int,
        @Query("start") index: Int
    ): Observable<Response<List<CollectionPOJO>>>

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

    @GET("users/{user}/deleted")
    fun getDeletedEntriesSince(
        @Path("user") user: String,
        @Query("since") since: Int
    ): Observable<Response<DeletedEntriesPojo>>

    @GET("groups/{groupID}/deleted")
    fun getDeletedEntriesForGroupSince(
        @Path("groupID") user: Int,
        @Query("since") since: Int
    ): Observable<Response<DeletedEntriesPojo>>

    @GET("users/{user}/items/trash")
    fun getTrashedItemsForUser(
        @Header("If-Modified-Since-Version") ifModifiedSinceVersion: Int,
        @Path("user") user: String,
        @Query("since") since: Int,
        @Query("start") index: Int
    ): Observable<Response<ResponseBody>>

    @GET("groups/{groupID}/items/trash")
    fun getTrashedItemsForGroup(
        @Header("If-Modified-Since-Version") ifModifiedSinceVersion: Int,
        @Path("groupID") groupID: Int,
        @Query("since") since: Int,
        @Query("start") index: Int
    ): Observable<Response<ResponseBody>>

    @GET("users/{user}/collections")
    fun getCollections(
        @Header("If-Modified-Since-Version") ifModifiedSinceVersion: Int,
        @Path("user") user: String,
        @Query("start") index: Int
    ): Observable<Response<List<CollectionPOJO>>>

    @Streaming
    @GET("users/{user}/items/{itemKey}/file")
    fun getFileForUser(
        @Path("user") user: String,
        @Path("itemKey") itemKey: String
    ): Observable<Response<ResponseBody>>

    @Streaming
    @GET("groups/{group}/items/{itemKey}/file")
    fun getFileForGroup(
        @Path("group") group: Int,
        @Path("itemKey") itemKey: String
    ): Observable<Response<ResponseBody>>

    @POST("users/{user}/items")
    fun writeItem(
        @Path("user") user: String,
        @Body json: JsonArray
    ): Call<ResponseBody>

    @POST("users/{user}/items")
    fun uploadNote(
        @Path("user") user: String,
        @Body json: JsonArray
    ): Observable<Response<ResponseBody>>

    @PATCH("users/{user}/items/{itemKey}")
    fun patchItem(
        @Path("user") user: String,
        @Path("itemKey") itemKey: String,
        @Body json: JsonObject,
        @Header("If-Unmodified-Since-Version") ifUnmodifiedSinceVersion: Int
    ): Observable<Response<ResponseBody>>

    @DELETE("users/{user}/items/{itemKey}")
    fun deleteItem(
        @Path("user") user: String,
        @Path("itemKey") itemKey: String,
        @Header("If-Unmodified-Since-Version") ifUnmodifiedSinceVersion: Int
    ): Call<ResponseBody>

    @POST("users/{user}/items/{itemKey}/file")
    fun uploadAttachmentAuthorization(
        @Path("user") user: String,
        @Path("itemKey") itemKey: String,
        @Query("md5") md5: String,
        @Query("filename") filename: String,
        @Query("filesize") filesize: Long,
        @Query("mtime") mtime: Long,
        @Query("params") params: Int = 1,
        @Body bodyText: String,
        @Header("If-Match") oldMd5Key: String
    ): Observable<Response<ResponseBody>>

    @POST("users/{user}/items/{itemKey}/file")
    fun registerUpload(
        @Path("user") user: String,
        @Path("itemKey") itemKey: String,
        @Query("upload") uploadKey: String,
        @Body body: String,
        @Header("If-Match") oldMd5Key: String
    ): Observable<Response<ResponseBody>>

    @POST
    fun uploadAttachmentToAmazon(
        @Url url: String,
        @Body data: RequestBody
    ): Observable<Response<ResponseBody>>

    @Multipart
    @POST
    fun uploadAttachmentToAmazonMulti(
        @Url url: String,
        @Part("key") key: RequestBody,
        @Part("acl") acl: RequestBody,
        @Part("Content-MD5") content_MD5: RequestBody,
        @Part("success_action_status") success_action_status: RequestBody,
        @Part("policy") policy: RequestBody,
        @Part("x-amz-algorithm") x_amz_algorithm: RequestBody,
        @Part("x-amz-credential") x_amz_credential: RequestBody,
        @Part("x-amz-date") x_amz_date: RequestBody,
        @Part("x-amz-signature") x_amz_signature: RequestBody,
        @Part("x-amz-security-token") x_amz_security_token: RequestBody,
        @Part("file") attachmentData: RequestBody
    ): Observable<Response<ResponseBody>>
}