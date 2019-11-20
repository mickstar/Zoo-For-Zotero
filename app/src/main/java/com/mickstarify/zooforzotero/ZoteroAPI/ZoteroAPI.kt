package com.mickstarify.zooforzotero.ZoteroAPI

import android.content.Context
import android.util.Log
import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.mickstarify.zooforzotero.BuildConfig
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.ZoteroAPI.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroAPI.Model.*
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
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
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.*


const val BASE_URL = "https://api.zotero.org"

class UpToDateException(message: String) : Exception(message)

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
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(ZoteroAPIService::class.java)
    }

    fun getFileForDownload(context: Context, item: Item): File {
        val name = item.data.get("filename") ?: "unknown.pdf"
        val outputDir = context.externalCacheDir

//        var extension = when (item.data["contentType"]) {
//            "application/pdf" -> "pdf"
//            "text/html" -> "html"
//            else -> ""
//        }
//        val filename = "${name}.${extension}"

        val file = File(outputDir, name)
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

    fun downloadItemWithWebDAV(
        context: Context,
        item: Item,
        listener: ZoteroAPIDownloadAttachmentListener
    ) {
        val preferenceManager = PreferenceManager(context)
        val webdav = Webdav(
            preferenceManager.getWebDAVAddress(),
            preferenceManager.getWebDAVUsername(),
            preferenceManager.getWebDAVPassword()
        )

        val task = doAsync {
            var outputFile: File? = null
            var hadIOError = false
            var illegalArgumentError = false
            try {
                outputFile = webdav!!.getAttachment(item.ItemKey, context)
            } catch (e: IOException) {
                hadIOError = true
            } catch (e: IllegalArgumentException) {
                illegalArgumentError = true

            } catch (e: Exception) {

            }

            onComplete {
                if (outputFile != null) {
                    listener.onComplete(outputFile)
                } else {
                    if (hadIOError) {
                        listener.onFailure("Error, ${item.ItemKey.toUpperCase()}.zip was not found on the webDAV server.")
                    } else if (illegalArgumentError) {
                        listener.onFailure("Error, your WebDAV is misconfigured. Please disable WebDAV in your settings, or reconfigure WebDAV from the menu.")
                    } else {
                        listener.onFailure()
                    }
                }
            }
        }
        listener.receiveTask(task)

    }

    fun downloadItem(
        context: Context,
        item: Item,
        useGroup: Boolean,
        groupID: Int = 0,
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

        val preferenceManager = PreferenceManager(context)
        // we will delegate to a webdav download if the user has webdav enabled.
        // When the user has webdav enabled on personal account or for groups are the only two conditions.
        if ((!useGroup && preferenceManager.isWebDAVEnabled()) || (useGroup && preferenceManager.isWebDAVEnabledForGroups())) {
            return downloadItemWithWebDAV(context, item, listener)
            //stops here.
        }

        val zoteroAPI = buildZoteroAPI(useCaching = false, libraryVersion = -1)
        val call: Call<ResponseBody> = if (useGroup) {
            zoteroAPI.getAttachmentFileFromGroup(groupID, item.ItemKey)
        } else {
            zoteroAPI.getItemFile(userID, item.ItemKey)
        }
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                val inputStream = response.body()?.byteStream()
                val fileSize = response.body()?.contentLength() ?: 0
                if (inputStream == null) {
                    listener.onNetworkFailure()
                    return
                }
                if (response.code() == 200) {
                    val task = doAsync {
                        val outputFileStream = outputFile.outputStream()
                        var failure = false
                        val buffer = ByteArray(32768)
                        var read = inputStream.read(buffer)
                        var progress: Long = 0
                        var priorProgress: Long = -1
                        while (read > 0) {
                            // I Should just bite the bullet and implement rxJava...
                            context.runOnUiThread {
                                if (priorProgress != progress) {
                                    listener.onProgressUpdate(progress, fileSize)
                                    priorProgress = progress
                                }
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
                } else {
                    listener.onNetworkFailure()
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

    fun getCollections(
        useCaching: Boolean,
        libraryVersion: Int,
        listener: ZoteroAPIDownloadCollectionListener
    ) {
        /*
        * getItems loads the Every Item from the Zotero API.
        * since Zotero API may only return partial data, we may need to make several requests so this may take
        * some time to get all the data.
        * We use recursion to download all the items.
        * */


        val collections = LinkedList<Collection>()
        val zoteroAPI = buildZoteroAPI(useCaching, libraryVersion)
        getCollectionsFromIndex(collections, 0, zoteroAPI, listener)
    }

    fun getItemsSinceModification(
        modificationSinceVersion: Int,
        listener: ZoteroAPIDownloadItemsListener
    ) {
        val modifiedItems = LinkedList<Item>()
        val zoteroAPI = buildZoteroAPI(false, 0)
        getItemsSinceModificationFromIndex(
            modifiedItems,
            modificationSinceVersion,
            0,
            zoteroAPI,
            listener
        )

    }

    private fun getItemsSinceModificationFromIndex(
        modifiedItems: MutableList<Item>,
        modificationSinceVersion: Int,
        index: Int,
        zoteroAPIService: ZoteroAPIService,
        listener: ZoteroAPIDownloadItemsListener
    ) {
        val call = zoteroAPIService.getItemsSince(userID, modificationSinceVersion, index)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.code() == 200) {
                    val s = response.body()?.string()
                    if (s == null) {
                        throw(Exception("Error on item call, got back nothing on call."))
                    }
                    val newItems = ItemJSONConverter().deserialize(s)

                    modifiedItems.addAll(newItems)

                    val totalResults: String? = response.headers()["Total-Results"]
                    val myLibraryVersion: Int =
                        response.headers()["Last-Modified-Version"]?.toInt() ?: -1
                    if (totalResults == null) {
                        listener.onDownloadComplete(modifiedItems, myLibraryVersion)
                    } else {
                        val newIndex = index + newItems.size
                        if (newIndex < totalResults.toInt()) {
                            listener.onProgressUpdate(newIndex, totalResults.toInt())
                            getItemsSinceModificationFromIndex(
                                modifiedItems,
                                modificationSinceVersion,
                                newIndex,
                                zoteroAPIService,
                                listener
                            )
                        } else {
                            listener.onDownloadComplete(modifiedItems, myLibraryVersion)
                        }
                    }
                } else {
                    Log.e(
                        "zotero",
                        "Error downloading modified since items, got back code ${response.code()} message: ${response.body()}"
                    )
                    listener.onNetworkFailure()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d("zotero", "network failure ${t.message}")
                listener.onNetworkFailure()
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
                    Log.e(
                        "zotero",
                        "Error downloading items, got back code ${response.code()} message: ${response.body()}"
                    )
                    listener.onNetworkFailure()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d("zotero", "network failure ${t.message}")
                listener.onNetworkFailure()
            }
        })
    }

    private fun getCollectionsFromIndex(
        collections: MutableList<Collection>,
        index: Int,
        zoteroAPIService: ZoteroAPIService,
        listener: ZoteroAPIDownloadCollectionListener
    ) {
        val call: Call<List<Collection>> = zoteroAPIService.getCollections(userID, index)

        call.enqueue(object : Callback<List<Collection>> {
            override fun onResponse(
                call: Call<List<Collection>>,
                response: Response<List<Collection>>
            ) {
                if (response.code() == 200) {
                    response.body()?.let {
                        collections.addAll(it)
                    }

                    val totalResults: String? = response.headers()["Total-Results"]
                    val myLibraryVersion: Int =
                        response.headers()["Last-Modified-Version"]?.toInt() ?: -1
                    if (totalResults == null) {
                        listener.onDownloadComplete(collections)
                    } else {
                        var newIndex = index
                        response.body()?.let {
                            newIndex += it.size
                        }
                        if (newIndex < totalResults.toInt()) {
                            listener.onProgressUpdate(newIndex, totalResults.toInt())
                            getCollectionsFromIndex(
                                collections,
                                newIndex,
                                zoteroAPIService,
                                listener
                            )
                        } else {
                            listener.onDownloadComplete(collections)
                        }
                    }
                } else if (response.code() == 304) {
                    Log.d("zotero", "got back 304, using cached copy.")
                    listener.onCachedComplete()
                } else {
                    Log.e(
                        "zotero",
                        "Error downloading collections, got back code ${response.code()} message: ${response.body()}"
                    )
                    listener.onNetworkFailure()
                }
            }

            override fun onFailure(call: Call<List<Collection>>, t: Throwable) {
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

    private fun getItemsFromGroupFromIndex(
        groupID: Int,
        useCaching: Boolean,
        modificationSinceVersion: Int,
        index: Int = 0
    ): Observable<ZoteroAPIItemsResponse> {
        val service = buildZoteroAPI(useCaching, modificationSinceVersion)

        val observable = if (modificationSinceVersion > -1) {
            service.getItemsForGroupSince(groupID, modificationSinceVersion, index)
        } else {
            service.getItemsForGroup(groupID, index)
        }

        return observable.map { response: Response<ResponseBody> ->
            if (response.code() == 304) {
                ZoteroAPIItemsResponse(true, LinkedList(), -1, -1)
            } else {
                val s = response.body()?.string() ?: "[]"
                val newItems = ItemJSONConverter().deserialize(s)
                val lastModifiedVersion = response.headers()["Last-Modified-Version"]?.toInt() ?: -1
                val totalResults = response.headers()["Total-Results"]?.toInt() ?: -1

                ZoteroAPIItemsResponse(false, newItems, lastModifiedVersion, totalResults)
            }
        }
    }

    fun getItemsFromGroup(
        groupID: Int,
        useCaching: Boolean,
        modificationSinceVersion: Int = -1,
        index: Int = 0
    ): Observable<ZoteroAPIItemsResponse> {

        val observable = Observable.create(object : ObservableOnSubscribe<ZoteroAPIItemsResponse> {
            var itemCount = index
            override fun subscribe(emitter: ObservableEmitter<ZoteroAPIItemsResponse>) {
                val observable =
                    getItemsFromGroupFromIndex(groupID, useCaching, modificationSinceVersion, index)
                observable.subscribe(object : Observer<ZoteroAPIItemsResponse> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onNext(response: ZoteroAPIItemsResponse) {
                        if (response.isCached) {
                            throw(UpToDateException("304. Already Downloaded."))
                        }
                        itemCount += response.items.size
                        emitter.onNext(
                            ZoteroAPIItemsResponse(
                                false,
                                response.items,
                                response.LastModifiedVersion,
                                response.totalResults
                            )
                        )
                        if (itemCount < response.totalResults) {
                            val s = getItemsFromGroup(
                                groupID,
                                useCaching,
                                modificationSinceVersion,
                                itemCount
                            )
                            s.blockingForEach(Consumer { t -> emitter.onNext(t) })
                        }
                        emitter.onComplete()
                    }

                    override fun onError(e: Throwable) {
                        emitter.onError(e)
                    }

                })
            }
        })
        return observable
    }

    private fun getCollectionForGroupFromIndex(
        groupID: Int,
        useCaching: Boolean,
        modificationSinceVersion: Int,
        index: Int = 0
    ): Observable<ZoteroAPICollectionsResponse> {
        val service = buildZoteroAPI(useCaching, modificationSinceVersion)

        val observable = service.getCollectionsForGroup(groupID, index)

        return observable.map { response: Response<List<Collection>> ->
            if (response.code() == 304) {
                throw UpToDateException("304 Collections up to date.")
            }

            val collections = response.body() ?: LinkedList()
            val totalResults = response.headers()["Total-Results"]?.toInt() ?: 0
            ZoteroAPICollectionsResponse(false, collections, totalResults)
        }
    }

    fun getCollectionsFromGroup(
        groupID: Int,
        libraryVersion: Int,
        useCaching: Boolean,
        index: Int = 0
    ): Observable<List<Collection>> {
        val observable = Observable.create(object : ObservableOnSubscribe<List<Collection>> {
            var itemCount = index
            override fun subscribe(emitter: ObservableEmitter<List<Collection>>) {
                val observable = getCollectionForGroupFromIndex(groupID, useCaching, index)
                observable.subscribe(object : Observer<ZoteroAPICollectionsResponse> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onNext(response: ZoteroAPICollectionsResponse) {
                        itemCount += response.collections.size
                        emitter.onNext(response.collections)
                        if (itemCount < response.totalResults) {
                            val s = getCollectionsFromGroup(
                                groupID,
                                libraryVersion,
                                useCaching,
                                itemCount
                            )
                            s.blockingForEach(Consumer { t -> emitter.onNext(t) })
                        }
                        emitter.onComplete()
                    }

                    override fun onError(e: Throwable) {
                        emitter.onError(e)
                    }

                })
            }
        })
        return observable
    }

    fun getGroupInfo(): Observable<List<GroupInfo>> {
        val service = buildZoteroAPI(true, -1)
        val groupInfo = service.getGroupInfo(userID)
        return groupInfo.map {
            it.map { groupPojo ->
                GroupInfo(
                    id = groupPojo.groupData.id,
                    version = groupPojo.version,
                    name = groupPojo.groupData.name,
                    description = groupPojo.groupData.description,
                    fileEditing = groupPojo.groupData.fileEditing,
                    libraryEditing = groupPojo.groupData.libraryEditing,
                    libraryReading = groupPojo.groupData.libraryReading,
                    owner = groupPojo.groupData.owner,
                    type = groupPojo.groupData.type,
                    url = groupPojo.groupData.url
                )
            }
        }
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