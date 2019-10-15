package com.mickstarify.zooforzotero.ZoteroAPI

import android.content.Context
import android.util.Log
import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.mickstarify.zooforzotero.BuildConfig
import com.mickstarify.zooforzotero.ZoteroAPI.Model.*
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.runOnUiThread
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.*


const val BASE_URL = "https://api.zotero.org"

class ZoteroAPI(
    val API_KEY: String,
    val userID: String,
    val username: String
) {
    private fun buildZoteroAPI(
        useCaching: Boolean,
        libraryVersion: Int,
        ifModifiedSinceVersion: Int = -1
    ): ZoteroAPIService {
        val httpClient = OkHttpClient().newBuilder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    this.level = HttpLoggingInterceptor.Level.BODY
                })
            }
            addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
                    val request = chain.request().newBuilder()
                        .addHeader("Zotero-API-Version", "3")
                        .addHeader("Zotero-API-Key", API_KEY)
                    if (useCaching && libraryVersion > 0) {
                        request.addHeader("If-Modified-Since-Version", "$libraryVersion")
                    }
                    if (ifModifiedSinceVersion >= 0) {
                        request.addHeader("If-Unmodified-Since-Version", "$ifModifiedSinceVersion")
                    }
                    return chain.proceed(request.build())
                }
            })
        }.build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ZoteroAPIService::class.java)
    }

    fun getCollections(
        useCaching: Boolean,
        libraryVersion: Int,
        listener: ZoteroAPIDownloadCollectionListener
    ) {
        /*Does a async call to the API and callbacks with the list of collections, returns an empty list on failure.*/

        val zoteroAPI = buildZoteroAPI(useCaching, libraryVersion)
        val call: Call<List<Collection>> = zoteroAPI.getCollections(userID)

        call.enqueue(object : Callback<List<Collection>> {
            override fun onResponse(
                call: Call<List<Collection>>,
                response: Response<List<Collection>>
            ) {
                if (response.code() == 200) {
                    val collections = response.body() ?: LinkedList()
                    listener.onDownloadComplete(collections)
                } else if (response.code() == 304) {
                    listener.onCachedComplete()
                } else {
                    Log.e("zotero", "Error downloading collections, got back code ${response.code()} message: ${response.body()}")
                    listener.onNetworkFailure()
                }
            }

            override fun onFailure(call: Call<List<Collection>>, t: Throwable) {
                Log.e("zotero", "failure on getting Collection message: ${t.message}")
                listener.onNetworkFailure()
            }
        })
    }

    fun getFileForDownload(context: Context, item: Item): File {
        val name = item.ItemKey.toUpperCase(Locale.getDefault())
        val outputDir = context.externalCacheDir

        var extension = when (item.data["contentType"]) {
            "application/pdf" -> "pdf"
            "text/html" -> "html"
            else -> ""
        }
        val filename = "${name}.${extension}"

        val file = File(outputDir, filename)
        return file
    }

    fun checkIfFileExists(file: File, item: Item): Boolean {
        if (file.exists()) {
            val md5 = item.data["md5"] ?: ""
            val md5Key = Files.hash(file, Hashing.md5()).toString()
            return md5Key == md5
        }
        return false
    }

    fun downloadItem(
        context: Context,
        item: Item,
        listener: ZoteroAPIDownloadAttachmentListener
    ) {
        if (item.getItemType() != "attachment") {
            Log.d("zotero", "got download request for item that isn't attachment")
        }

        val outputFile = getFileForDownload(context, item)
        if (checkIfFileExists(outputFile, item)) {
            listener.onComplete(outputFile)
            return
        }

        val zoteroAPI = buildZoteroAPI(useCaching = false, libraryVersion = -1)

        val call: Call<ResponseBody> = zoteroAPI.getItemFile(userID, item.ItemKey)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.code() == 200) {
                    val inputStream = response.body()?.byteStream()
                    val fileSize = response.body()?.contentLength() ?: 0
                    if (inputStream == null) {
                        listener.onNetworkFailure()
                        return
                    }
                    val task = doAsync {
                        val outputFileStream = outputFile.outputStream()
                        val buffer = ByteArray(32768)
                        var read = inputStream.read(buffer)
                        var progress: Long = 0
                        var failure = false
                        while (read > 0) {
                            // I Should just bite the bullet and implement rxJava...
                            context.runOnUiThread {
                                listener.onProgressUpdate(progress, fileSize)
                            }
                            try {
                                outputFileStream.write(buffer, 0, read)
                                read = inputStream.read(buffer)
                            } catch (e: java.io.InterruptedIOException) {
                                outputFileStream.close()
                                inputStream.close()
                                failure = true
                                break
                            }
                            progress += read
                        }
                        if (read > 0) {
                            failure = true
                        }

                        onComplete {
                            outputFileStream.close()
                            inputStream.close()
                            if (failure == false) {
                                listener.onComplete(outputFile)
                            }
                        }
                    }
                    listener.receiveTask(task)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("zotero", "Download Items(1) Failure, message: ${t.message}")
                listener.onNetworkFailure()
            }
        })
    }

    fun testConnection(callback: (success: Boolean, message: String) -> (Unit)) {
        println("testConnection()")
        val zoteroAPI = buildZoteroAPI(useCaching = false, libraryVersion = -1)
        val call: Call<KeyInfo> = zoteroAPI.getKeyInfo(this.API_KEY)

        call.enqueue(object : Callback<KeyInfo> {
            override fun onResponse(call: Call<KeyInfo>, response: Response<KeyInfo>) {
                if (response.code() == 200) {
                    Log.d("zotero", "Successfully tested connection.")
                    callback(true, "")
                } else {
                    callback(
                        false,
                        "error got back response code ${response.code()} body: ${response.body()}"
                    )
                }
            }

            override fun onFailure(call: Call<KeyInfo>, t: Throwable) {
                Log.d("zotero", "failure on item")
                callback(false, "Failure.")
            }
        })
    }

    fun getItems(
        useCaching: Boolean,
        libraryVersion: Int,
        listener: ZoteroAPIDownloadItemsListener
    ) {
        /*
        * getItems loads the Every Item from the Zotero API.
        * since Zotero API may only return partial data, we may need to make several requests so this may take
        * some time to get all the data.
        * We use recursion to download all the items.
        * */


        val items = LinkedList<Item>()
        val zoteroAPI = buildZoteroAPI(useCaching, libraryVersion)
        getItemsFromIndex(items, 0, zoteroAPI, listener)
    }


    private fun getItemsFromIndex(
        items: MutableList<Item>,
        index: Int,
        zoteroAPIService: ZoteroAPIService,
        listener: ZoteroAPIDownloadItemsListener
    ) {
        val call: Call<ResponseBody> = zoteroAPIService.getItems(userID, index)

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
                    val myLibraryVersion: Int =
                        response.headers()["Last-Modified-Version"]?.toInt() ?: -1
                    if (totalResults == null) {
                        listener.onDownloadComplete(items, myLibraryVersion)
                    } else {
                        val newIndex = index + newItems.size
                        if (newIndex < totalResults.toInt()) {
                            listener.onProgressUpdate(newIndex, totalResults.toInt())
                            getItemsFromIndex(items, newIndex, zoteroAPIService, listener)
                        } else {
                            listener.onDownloadComplete(items, myLibraryVersion)
                        }
                    }
                } else if (response.code() == 304) {
                    listener.onCachedComplete()

                } else {
                    Log.e("zotero", "Error downloading items, got back code ${response.code()} message: ${response.body()}")
                    listener.onNetworkFailure()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d("zotero", "network failure ${t.message}")
                listener.onNetworkFailure()
            }
        })
    }

    fun uploadNote(note: Note) {
        val zoteroAPI = buildZoteroAPI(useCaching = false, libraryVersion = -1)
        val call: Call<ResponseBody> = zoteroAPI.writeItem(userID, note.asJsonArray())

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.code() == 200) {
                    Log.d("zotero", "success on note upload")
                } else {
                    Log.d("zotero", "got back code ${response.code()} from note upload.")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            }
        })
    }

    fun modifyNote(note: Note, libraryVersion: Int) {
        val zoteroAPI = buildZoteroAPI(true, -1, note.version)
        val call: Call<ResponseBody> = zoteroAPI.editNote(userID, note.key, note.getJsonNotePatch())

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.code() == 200 || response.code() == 204) {
                    Log.d("zotero", "success on note modification")
                } else {
                    Log.d("zotero", "got back code ${response.code()} from note upload.")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            }
        })
    }

    fun deleteItem(itemKey: String, version: Int, listener: DeleteItemListener) {
        val zoteroAPI = buildZoteroAPI(true, -1, version)
        val call: Call<ResponseBody> = zoteroAPI.deleteItem(userID, itemKey)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                when (response.code()) {
                    204 -> listener.success()
                    409 -> listener.failedItemLocked()
                    412 -> listener.failedItemChangedSince()
                    else -> listener.failed(response.code())
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            }
        })
    }

    fun uploadPDF(parent: Item, attachment: File) {
//        val zoteroAPI = buildZoteroAPI(useCaching = false, libraryVersion = -1)
//        val call: Call<ResponseBody> = zoteroAPI.writeItem(userID, note.asJsonArray())
//
//        call.enqueue(object : Callback<ResponseBody> {
//            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
//                if (response.code() == 200) {
//                    Log.d("zotero", "success on note upload")
//                } else {
//                    Log.d("zotero", "got back code ${response.code()} from note upload.")
//                }
//            }
//
//            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//            }
//        })
    }
}