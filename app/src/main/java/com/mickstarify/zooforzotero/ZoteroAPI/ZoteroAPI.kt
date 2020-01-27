package com.mickstarify.zooforzotero.ZoteroAPI

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mickstarify.zooforzotero.BuildConfig
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.ZoteroAPI.Model.*
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ItemsDownloadProgress
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit


const val BASE_URL = "https://api.zotero.org"

class UpToDateException(message: String) : RuntimeException(message)
class LibraryVersionMisMatchException(message: String) : RuntimeException(message)
class APIKeyRevokedException(message: String) : RuntimeException(message)
class AlreadyUploadedException(message: String) : RuntimeException(message)
class PreconditionFailedException(message: String) : RuntimeException(message)
class RequestEntityTooLarge(message: String) : RuntimeException(message)
class ServerAlreadyExistsException(message: String) : RuntimeException(message)
class ItemLockedException(message: String) : RuntimeException(message)
class ItemChangedSinceException(message: String) : RuntimeException(message)
class ZoteroNotFoundException(message: String) : RuntimeException(message)
class ZoteroAPI(
    val API_KEY: String,
    val userID: String,
    val username: String,
    val attachmentStorageManager: AttachmentStorageManager
) {
    val httpClient = OkHttpClient().newBuilder().apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().apply {
                this.level = HttpLoggingInterceptor.Level.BODY
            })
        }
        writeTimeout(
            10,
            TimeUnit.MINUTES
        ) // so socket doesn't timeout on large uploads (attachments)
        addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
                val request = chain.request().newBuilder()
                    .addHeader("Zotero-API-Version", "3")
                    .addHeader("Zotero-API-Key", API_KEY)
                return chain.proceed(request.build())
            }
        })
    }.build()

    val service = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build().create(ZoteroAPIService::class.java)

    fun buildAmazonService(): ZoteroAPIService {
        val httpClient = OkHttpClient().newBuilder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    this.level = HttpLoggingInterceptor.Level.BODY
                })
            }
            writeTimeout(
                10,
                TimeUnit.MINUTES
            ) // so socket doesn't timeout on large uploads (attachments)
        }.build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(ZoteroAPIService::class.java)
    }

    fun downloadItemRx(
        item: Item,
        groupID: Int = GroupInfo.NO_GROUP_ID,
        context: Context
    ): Observable<DownloadProgress> {

        val useGroup = groupID != GroupInfo.NO_GROUP_ID
        if (item.itemType != "attachment") {
            Log.d("zotero", "got download request for item that isn't attachment")
        }

        val preferenceManager = PreferenceManager(context)

        // we will delegate to a webdav download if the user has webdav enabled.
        // When the user has webdav enabled on personal account or for groups are the only two conditions.
        if (!useGroup && preferenceManager.isWebDAVEnabled() || (useGroup && preferenceManager.isWebDAVEnabledForGroups())) {
            val webdav = Webdav(
                preferenceManager.getWebDAVAddress(),
                preferenceManager.getWebDAVUsername(),
                preferenceManager.getWebDAVPassword()
            )

            return webdav.downloadFileRx(item, context, attachmentStorageManager)
            //stops here.
        }


        val outputFileStream = attachmentStorageManager.getItemOutputStream(item)

        val observable = Observable.create<DownloadProgress> { emitter ->

            val downloader = if(!useGroup) {
                service.getFileForUser(userID, item.itemKey)
            } else {
                service.getFileForGroup(groupID, item.itemKey)
            }
            downloader.subscribe(object : Observer<Response<ResponseBody>> {
                val disposable: Disposable? = null
                override fun onComplete() {
                    emitter.onComplete()
                }

                override fun onSubscribe(d: Disposable) {
                    // do nothing.
                }

                override fun onNext(response: Response<ResponseBody>) {
                    if (emitter.isDisposed == true) {
                        Log.d("zotero", "download was cancelled, not writing.")
                        return
                    }

                    val inputStream = response.body()?.byteStream()
                    val fileSize = response.body()?.contentLength() ?: 0
                    if (response.code() == 200) {
                        val buffer = ByteArray(64768)
                        var read = inputStream?.read(buffer) ?: 0
                        var progress: Long = 0
                        val md5Hash = item.data["md5"] ?: ""
                        val mtime = (item.data["mtime"] ?: "0").toLong()
                        while (read > 0) {
                            try {
                                progress += read
                                emitter.onNext(
                                    DownloadProgress(
                                        progress = progress,
                                        total = fileSize,
                                        metadataHash = md5Hash,
                                        mtime = mtime
                                    )
                                )
                                outputFileStream.write(buffer, 0, read)
                                read = inputStream?.read(buffer) ?: 0
                            } catch (e: java.io.InterruptedIOException) {
                                outputFileStream.close()
                                inputStream?.close()
                                throw (e)
                            }
                            progress += read
                        }
                        emitter.onComplete()
                    } else if (response.code() == 404) {
                        throw ZoteroNotFoundException("Not found on server.")
                    } else {
                        Log.e("zotero", "network error. response: ${response.body()}")
                        throw RuntimeException("Invalid server response code ${response.code()}")
                    }
                }

                override fun onError(e: Throwable) {
                    Log.e("zotero", "download attachment got error $e  ${emitter.isDisposed}")
                    attachmentStorageManager.deleteAttachment(item)
                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                    }
                }
            })
        }
        return observable
    }

    fun testConnection(callback: (success: Boolean, message: String) -> (Unit)) {
        println("testConnection()")
        val call: Call<KeyInfo> = service.getKeyInfo(this.API_KEY)

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

    fun modifyNote(note: Note, libraryVersion: Int): Completable {
        val observable =
            service.patchItem(userID, note.key, note.getJsonNotePatch(), note.version)
                .map { response ->
                    if (response.code() == 200 || response.code() == 204) {
                        Log.d("zotero", "success on note modification")
                    } else if (response.code() == 409) {
                        throw ItemLockedException("Failed to upload note.")
                    } else if (response.code() == 412) {
                        throw ItemChangedSinceException("item out of date, please sync first.")
                    } else {
                        Log.d("zotero", "got back code ${response.code()} from note upload.")
                        throw Exception("Server gave back code ${response.code()}")
                    }
                    response
                }
        return Completable.fromObservable(observable)
    }

    fun uploadNote(note: Note): Completable {
        val observable = service.uploadNote(userID, note.asJsonArray()).map {
            if (it.code() == 200 || it.code() == 204) {
                // success
            } else {
                throw Exception("Wrong server response code. ${it.code()}")
            }
            it
        }

        return Completable.fromObservable(observable)
    }

    fun getDeletedEntries(
        sinceVersion: Int,
        groupID: Int = GroupInfo.NO_GROUP_ID
    ): Single<DeletedEntriesPojo> {
        val observable = when (groupID) {
            GroupInfo.NO_GROUP_ID -> {
                service.getDeletedEntriesSince(userID, sinceVersion)
            }
            else -> {
                throw NotImplementedError()
            }
        }
        return Single.fromObservable(observable.map { response ->
            if (response.code() == 200) {
                response.body()
            } else {
                throw Exception("Error server responded with code ${response.code()}")
            }
        })
    }

    fun deleteItem(itemKey: String, version: Int, listener: DeleteItemListener) {
        val call: Call<ResponseBody> = service.deleteItem(userID, itemKey, version)

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

    fun patchItem(item: Item, patch: JsonObject): Completable {
        val lastModifiedVersion = item.getVersion()
        val observable =
            service.patchItem(userID, item.itemKey, patch, lastModifiedVersion).map { response ->
                if (response.code() == 200 || response.code() == 204) {
                    Log.d("zotero", "success on patch")
                } else if (response.code() == 409) {
                    throw ItemLockedException("You do not have write permission.")
                } else if (response.code() == 412) {
                    throw ItemChangedSinceException("Local item out of date, please sync first.")
                } else {
                    Log.d("zotero", "Zotero server gave code ${response.code()} for patch.")
                    throw Exception("Zotero server gave back code ${response.code()}")
                }
            }
        return Completable.fromObservable(observable)
    }

    private fun getItemsFromIndex(
        modificationSinceVersion: Int,
        index: Int,
        groupID: Int
    ): Observable<ZoteroAPIItemsResponse> {

        val observable = if (modificationSinceVersion > -1) {
            if (groupID != GroupInfo.NO_GROUP_ID) {
                service.getItemsForGroupSince(
                    modificationSinceVersion,
                    groupID,
                    modificationSinceVersion,
                    index
                )
            } else {
                service.getItemsSince(
                    modificationSinceVersion,
                    userID,
                    modificationSinceVersion,
                    index
                )
            }
        } else {
            if (groupID != GroupInfo.NO_GROUP_ID) {
                service.getItemsForGroup(0, groupID, index)
            } else {
                service.getItems(0, userID, index)
            }

        }

        Log.d("zotero", "got since=$modificationSinceVersion")

        return observable.map { response: Response<ResponseBody> ->
            if (response.code() == 304) {
                throw UpToDateException("304 Items already loaded.")
            } else if (response.code() == 200) {
                val s = response.body()?.string() ?: "[]"
                val newItems = ItemJSONConverter().deserialize(s)
                val lastModifiedVersion = response.headers()["Last-Modified-Version"]?.toInt() ?: -1
                val totalResults = response.headers()["Total-Results"]?.toInt() ?: -1

                ZoteroAPIItemsResponse(false, newItems, lastModifiedVersion, totalResults)
            } else {
                throw Exception("Server gave back ${response.code()} message: ${response.body()}")
            }
        }
    }

    fun getItems(
        libraryVersion: Int = -1,
        groupID: Int = GroupInfo.NO_GROUP_ID,
        downloadProgress: ItemsDownloadProgress? = null
    ): Observable<ZoteroAPIItemsResponse> {

        val isGroup = groupID != GroupInfo.NO_GROUP_ID

        val fromIndex = downloadProgress?.nDownloaded ?: 0

        val observable = Observable.create(object : ObservableOnSubscribe<ZoteroAPIItemsResponse> {
            override fun subscribe(emitter: ObservableEmitter<ZoteroAPIItemsResponse>) {
                var itemCount = fromIndex
                val s = getItemsFromIndex(
                    libraryVersion,
                    itemCount,
                    groupID
                )
                val response = s.blockingSingle()
                if (downloadProgress != null && response.LastModifiedVersion != downloadProgress.libraryVersion) {
                    // Due to possible miss-syncs, we cannot continue the download.
                    // we will raise an exception which will tell the activity to redownload without the
                    // progress object.
                    throw LibraryVersionMisMatchException("Cannot continue, our version ${downloadProgress.libraryVersion} doesn't match Server's ${response.LastModifiedVersion}")
                }

                val total = response.totalResults
                itemCount += response.items.size
                emitter.onNext(response)
                while (itemCount < total) {
                    val res = getItemsFromIndex(
                        libraryVersion,
                        itemCount,
                        groupID
                    ).blockingSingle()
                    itemCount += res.items.size
                    emitter.onNext(res)
                }
                emitter.onComplete()
            }
        })
        return observable
    }


    private fun getCollectionFromIndex(
        groupID: Int,
        modificationSinceVersion: Int,
        index: Int = 0
    ): Observable<ZoteroAPICollectionsResponse> {
        /* Obvservable that gets collections from a certain index. */

        val observable = if (groupID != GroupInfo.NO_GROUP_ID) {
            service.getCollectionsForGroup(modificationSinceVersion, groupID, index)
        } else {
            service.getCollections(modificationSinceVersion, userID, index)
        }

        return observable.map { response: Response<List<CollectionPOJO>> ->
            if (response.code() == 304) {
                throw UpToDateException("304 Collections up to date.")
            } else if (response.code() == 403) {
                throw APIKeyRevokedException("403 Api Key Invalid.")
            }

            val collections = response.body() ?: LinkedList()
            val totalResults = response.headers()["Total-Results"]?.toInt() ?: 0
            ZoteroAPICollectionsResponse(false, collections, totalResults)
        }
    }

    fun getCollections(
        libraryVersion: Int,
        groupID: Int = GroupInfo.NO_GROUP_ID
    ): Observable<List<CollectionPOJO>> {
        /* This method provides access to the zotero api collections endpoint.
        *  It allows for both user personal access as well as shared collections (group) access.
        *  You must specify useGroup, as well as provide a groupID*/

        val observable = Observable.create(object : ObservableOnSubscribe<List<CollectionPOJO>> {
            override fun subscribe(emitter: ObservableEmitter<List<CollectionPOJO>>) {

                val s = getCollectionFromIndex(
                    groupID,
                    libraryVersion
                )
                val response = s.blockingSingle()
                val total = response.totalResults
                emitter.onNext(response.collections)
                var itemCount = response.collections.size
                while (itemCount < total) {
                    val r = getCollectionFromIndex(
                        groupID,
                        libraryVersion,
                        index = itemCount
                    ).blockingSingle()
                    itemCount += r.collections.size
                    emitter.onNext(r.collections)
                }
                emitter.onComplete()
            }
        })
        return observable
    }

    fun getGroupInfo(): Observable<List<GroupInfo>> {
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

    fun getTrashedItemsFromIndex(groupID: Int, sinceVersion: Int, index: Int): Observable<ZoteroAPIItemsResponse> {
        val call = when(groupID){
            GroupInfo.NO_GROUP_ID -> {service.getTrashedItemsForUser(sinceVersion, userID, sinceVersion, index)}
            else -> {service.getTrashedItemsForGroup(sinceVersion, groupID, sinceVersion, index)}
        }

        return call.map {response ->
            if (response.code() == 304) {
                throw UpToDateException("304 trash already up to date.")
            } else if (response.code() == 200) {
                val s = response.body()?.string() ?: "[]"
                val newItems = ItemJSONConverter().deserialize(s)
                val lastModifiedVersion = response.headers()["Last-Modified-Version"]?.toInt() ?: -1
                val totalResults = response.headers()["Total-Results"]?.toInt() ?: -1

                ZoteroAPIItemsResponse(false, newItems, lastModifiedVersion, totalResults)
            } else {
                throw Exception("getTrash() Server gave back ${response.code()} message: ${response.body()}")
            }
        }
    }

    fun getTrashedItems(groupID: Int, sinceVersion: Int): Observable<ZoteroAPIItemsResponse> {


        val observable = Observable.create(object : ObservableOnSubscribe<ZoteroAPIItemsResponse> {
            override fun subscribe(emitter: ObservableEmitter<ZoteroAPIItemsResponse>) {
                var itemCount = 0
                val s = getTrashedItemsFromIndex(
                    groupID,
                    sinceVersion,
                    itemCount
                )
                val response = s.blockingSingle()
                val total = response.totalResults
                itemCount += response.items.size
                emitter.onNext(response)
                while (itemCount < total) {
                    val res = getTrashedItemsFromIndex(
                        groupID,
                        sinceVersion,
                        itemCount
                    ).blockingSingle()
                    itemCount += res.items.size
                    if (res.items.size == 0){
                        throw(RuntimeException("Error, zotero api did not respond with any items for /trash/ $itemCount of ${res.totalResults}"))
                    }
                    emitter.onNext(res)
                }
                emitter.onComplete()
            }
        })

        return observable
    }

    fun uploadAttachmentWithWebdav(attachment: Item, context: Context): Completable {
        val preferenceManager = PreferenceManager(context)
        val webdav = Webdav(
            preferenceManager.getWebDAVAddress(),
            preferenceManager.getWebDAVUsername(),
            preferenceManager.getWebDAVPassword()
        )
        return webdav.uploadAttachment(attachment, attachmentStorageManager)
    }

    fun updateAttachment(attachment: Item): Completable {
        val attachmentUri: Uri = attachmentStorageManager.getAttachmentUri(attachment)
        var oldMd5 = attachment.getMd5Key()
        if (oldMd5 == "") {
            Log.d("zotero", "no md5key provided for Item: ${attachment.getTitle()}")
            oldMd5 = "*"
        }

        val newMd5 = attachmentStorageManager.calculateMd5(attachment)
        if (oldMd5 == newMd5) {
            throw AlreadyUploadedException("Local attachment version is the same as Zotero's.")
        }
        val mtime = attachmentStorageManager.getMtime(attachment)
        val filename = attachmentStorageManager.getFilenameForItem(attachment)
        val filesize = attachmentStorageManager.getFileSize(attachmentUri)

        val authorizationObservable =
            getUploadAuthorization(attachment, oldMd5, newMd5, filename, filesize, mtime, service)


        val chain = authorizationObservable.map { authorizationPojo ->
            Log.d("zotero", "t ${authorizationPojo.uploadKey}")
            Log.d("zotero", "about to upload ${authorizationPojo.uploadKey}")
            val requestBody = RequestBody.create(
                "multipart/form-data".toMediaType(),
                attachmentStorageManager.readBytes(
                    attachment
                )
            )
            buildAmazonService().uploadAttachmentToAmazonMulti(
                authorizationPojo.url,
                authorizationPojo.params.key
                    .toRequestBody("multipart/form-data".toMediaType()),
                authorizationPojo.params.acl
                    .toRequestBody("multipart/form-data".toMediaType()),
                authorizationPojo.params.content_MD5
                    .toRequestBody("multipart/form-data".toMediaType()),
                authorizationPojo.params.success_action_status
                    .toRequestBody("multipart/form-data".toMediaType()),
                authorizationPojo.params.policy
                    .toRequestBody("multipart/form-data".toMediaType()),
                authorizationPojo.params.x_amz_algorithm
                    .toRequestBody("multipart/form-data".toMediaType()),
                authorizationPojo.params.x_amz_credential
                    .toRequestBody("multipart/form-data".toMediaType()),
                authorizationPojo.params.x_amz_date
                    .toRequestBody("multipart/form-data".toMediaType()),
                authorizationPojo.params.x_amz_signature
                    .toRequestBody("multipart/form-data".toMediaType()),
                authorizationPojo.params.x_amz_security_token
                    .toRequestBody("multipart/form-data".toMediaType()),
                requestBody
            ).flatMap { amazonResponse ->
                if (amazonResponse.code() == 421) {
                    throw PreconditionFailedException("412 Precondition failed when uploading ${attachment.itemKey}")
                }
                if (amazonResponse.code() == 201) {
                    // SUCCESS
                    service.registerUpload(
                        userID,
                        attachment.itemKey,
                        authorizationPojo.uploadKey,
                        "upload=${authorizationPojo.uploadKey}",
                        oldMd5
                    )
                } else {
                    throw RuntimeException("Amazon Attachment Server Gave server error: ${amazonResponse.code()}")
                }
            }.blockingSingle()
        }

        return Completable.create({ emitter ->
            chain.subscribe(object : Observer<Response<ResponseBody>> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(zoteroRegisterResponse: Response<ResponseBody>) {
                    when (zoteroRegisterResponse.code()) {
                        204 -> {
                            emitter.onComplete()
                        }
                        412 -> {
                            throw PreconditionFailedException("register upload returned")
                        }
                        413 -> {
                            throw RequestEntityTooLarge("Your file is too large")
                        }
                        else -> {
                            throw Exception("Zotero server replied: ${zoteroRegisterResponse.code()}")
                        }
                    }
                }

                override fun onError(e: Throwable) {
                    Log.e("zotero", "chain subscriber got error ${e}")
                    emitter.onError(e)
                }

            })
        })
    }

    private fun getUploadAuthorization(
        item: Item,
        oldMd5: String,
        newMd5: String,
        filename: String,
        filesize: Long,
        mtime: Long,
        service: ZoteroAPIService
    ): Observable<ZoteroUploadAuthorizationPojo> {
        Log.d("zotero", "upload requested, ${item.itemKey}")
        val observable = service.uploadAttachmentAuthorization(
            userID,
            item.itemKey,
            newMd5,
            filename,
            filesize, //in bytes
            mtime, //this is in milli
            1,
            "Content-Type: application/x-www-form-urlencoded",
            oldMd5
        )
        return observable.map { response ->
            if (response.code() == 200) {
                val jsonString = response.body()!!.string()
                if (jsonString == "{\"exists\":1}") {
                    throw AlreadyUploadedException("File already uploaded")
                } else {
                    Gson().fromJson(
                        jsonString,
                        ZoteroUploadAuthorizationPojo::class.java
                    )
                }
            } else {
                throw Exception("Server Response: ${response.code()} ${response.body()}")
            }
        }
    }
}