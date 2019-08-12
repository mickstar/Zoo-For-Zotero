package com.mickstarify.zooforzotero.ZoteroAPI

import android.util.Log
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import com.mickstarify.zooforzotero.ZoteroAPI.Model.ItemJSONConverter
import com.mickstarify.zooforzotero.ZoteroAPI.Model.KeyInfo
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.util.*

const val BASE_URL = "https://api.zotero.org"

class ZoteroAPI(
    val API_KEY: String,
    val userID: String,
    val username: String,
    val libraryVersion: Int
) {
    private var httpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(ZoteroAPIInterceptor(API_KEY))
        .addInterceptor(HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.HEADERS
        })
        .addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
                val request = chain.request().newBuilder()
                    .addHeader("Zotero-API-Version", "3")
                    .addHeader("Zotero-API-Key", API_KEY)

                if (libraryVersion > 0){
                    request.addHeader("If-Modified-Since-Version", "$libraryVersion")
                }
                return chain.proceed(request.build())
            }
        })
        .build()

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val zoteroAPI = retrofit.create(ZoteroAPIService::class.java)

    fun getCollections(callback: (statusCode : Int, collections: List<Collection>) -> (Unit)) {
        /*Does a async call to the API and callbacks with the list of collections, returns an empty list on failure.*/
        val call: Call<List<Collection>> = zoteroAPI.getCollections(userID)

        call.enqueue(object : Callback<List<Collection>> {
            override fun onResponse(call: Call<List<Collection>>, response: Response<List<Collection>>) {
                if (response.code() == 200) {
                    callback(200, response.body() ?: LinkedList())
                } else if (response.code() == 304){
                    callback(304, LinkedList())
                }else {
                    callback(404, LinkedList())
                }
            }

            override fun onFailure(call: Call<List<Collection>>, t: Throwable) {
                Log.d("zotero", "failure on getting Collection ${t.message}")
                callback(404, LinkedList())
            }
        })
    }

    fun testConnection(callback: (success: Boolean, message: String) -> (Unit)) {
        println("testConnection()")
        val call: Call<KeyInfo> = zoteroAPI.getKeyInfo(this.API_KEY)

        call.enqueue(object : Callback<KeyInfo> {
            override fun onResponse(call: Call<KeyInfo>, response: Response<KeyInfo>) {
                if (response.code() == 200) {
                    Log.d("zotero", "Successfully tested connection.")
                    callback(true, "")
                } else {
                    callback(false, "error got back response code ${response.code()} body: ${response.body()}")
                }
            }

            override fun onFailure(call: Call<KeyInfo>, t: Throwable) {
                Log.d("zotero", "failure on item")
                callback(false, "Failure.")
            }
        })
    }

    fun getItems(callback: (status: Int, libraryVersion: Int, items: List<Item>) -> (Unit)) {
        /*
        * getItems loads the Every Item from the Zotero API.
        * You must pass a callback function which will be called when the data is loaded.
        * since Zotero API may only return partial data, we may need to make several requests so this may take a few
        * seconds to get a callback.
        *
        * The callback will return a status code and a list of items.
        * Status Codes:
        *   200 - Everything worked. THe items will be populated fully.
        *   304 - There has been no update since the supplied version number, so no items were fetched.
        *   404 - There was some network error. An empty list will accompany this.
        *
        * The design is a bit of a pain but so is the zotero api lol.
        * */


        val items = LinkedList<Item>()
        getItemsFromIndex(items, 0, callback)
    }


    fun getItemsFromIndex(
        items: MutableList<Item>,
        index: Int, callback: (status: Int, libraryVersion: Int, items: List<Item>) -> (Unit)
    ) {
        /*Does a async call to the API and callbacks with the list of collections, returns an empty list on failure.*/
        val call: Call<ResponseBody> = zoteroAPI.getItems(userID, index, libraryVersion)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.code() == 200) {
                    val s = response.body()?.string()
                    if (s == null) {
                        throw(Exception("Error on item call, got back nothing on call."))
                    }
                    val newItems = ItemJSONConverter().deserialize(s)

                    items.addAll(newItems)

                    val totalResults: String? = response.headers()["Total-Results"]
                    val myLibraryVersion : Int = response.headers()["Last-Modified-Version"]?.toInt()?:-1
                    if (totalResults == null) {
                        callback(200, myLibraryVersion, items)
                    } else {
                        val newIndex = index + newItems.size
                        if (newIndex < totalResults.toInt()) {
                            getItemsFromIndex(items, newIndex, callback)
                        } else {
                            callback(200, myLibraryVersion, items)
                        }
                    }
                } else if (response.code() == 304) {
                    callback(304, -1, items) // items is empty, the user will not use it.

                } else {
                    callback(404, -1, LinkedList())
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d("zotero", "failure on getting Collection ${t.message}")
                callback(404, -1, LinkedList())
            }
        })
    }

}