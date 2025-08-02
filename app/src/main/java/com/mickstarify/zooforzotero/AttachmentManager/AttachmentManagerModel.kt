package com.mickstarify.zooforzotero.AttachmentManager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import com.mickstarify.zooforzotero.ZoteroAPI.DownloadProgress
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroAPI
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.AttachmentInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroDB
import com.mickstarify.zooforzotero.di.SingletonComponentsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.operators.observable.ObservableFromIterable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.LinkedList

class AttachmentManagerModel(val presenter: Contract.Presenter, val context: Context) :
    Contract.Model {

    lateinit var zoteroAPI: ZoteroAPI
    lateinit var zoteroDB: ZoteroDB

    val zoteroDatabase: ZoteroDatabase
    val attachmentStorageManager: AttachmentStorageManager
    val preferenceManager: PreferenceManager

    override var isDownloading = false // useful for button state.

    init {
        val appContext = (context as Activity).applicationContext
        val hiltEntryPoint = EntryPointAccessors.fromApplication(appContext, SingletonComponentsEntryPoint::class.java)
        zoteroDatabase = hiltEntryPoint.getZoteroDatabase()
        attachmentStorageManager = hiltEntryPoint.getAttachmentStorageManager()
        preferenceManager = hiltEntryPoint.getPreferences()

        val auth = AuthenticationStorage(context)
        if (auth.hasCredentials()) {
            zoteroAPI = ZoteroAPI(
                auth.getUserKey(),
                auth.getUserID(),
                auth.getUsername(),
                attachmentStorageManager,
                preferenceManager
            )
        } else {
            presenter.createErrorAlert(
                "No credentials Available",
                "There is no credentials available to connect " +
                        "to the zotero server. Please relogin to the app (clear app data if necessary)"
            ) { (context.finish()) }
        }

        if (!preferenceManager.hasShownCustomStorageWarning() && attachmentStorageManager.storageMode == AttachmentStorageManager.StorageMode.CUSTOM) {
            preferenceManager.setShownCustomStorageWarning(true)
            presenter.createErrorAlert(
                "custom storage selected",
                "Android has imposed limitations on how the filesystem can be accessed. This will mean much slower file access compared to using the " +
                        "external cache option."
            ) {}
        }
    }

    data class downloadAllProgress(
        var status: Boolean,
        var currentIndex: Int,
        var attachment: Item
    )

    override fun cancelDownload() {
        Log.d("zotero", "canceling download")
        isDownloading = false
        downloadDisposable?.dispose()
        presenter.finishLoadingAnimation()
    }

    var downloadDisposable: Disposable? = null

    override fun downloadAttachments() {
        if (isDownloading) {
            Log.e("zotero", "Error already downloading")
            return
        }
        // just incase it's still running.
        this.stopCalculateMetaInformation()
        isDownloading = true

        val toDownload = LinkedList<Item>()

        Completable.fromAction {
            for (attachment in zoteroDB.items!!.filter { it.itemType == "attachment" && it.data["linkMode"] != "linked_file" }) {
                if (attachment.isDownloadable()) {
                    toDownload.add(attachment)
                }
            }
        }.subscribeOn(Schedulers.io()).andThen(ObservableFromIterable(toDownload.withIndex()).map {
            val i = it.index
            val attachment = it.value
            var status = true

            if (!attachmentStorageManager.checkIfAttachmentExists(attachment, checkMd5 = false)) {
                zoteroAPI.downloadItemRx(attachment, zoteroDB.groupID, context)
                    .blockingSubscribe(object : Observer<DownloadProgress> {
                        var setMetadata = false
                        override fun onComplete() {
                            // do nothing.
                        }

                        override fun onSubscribe(d: Disposable) {
                        }

                        override fun onNext(it: DownloadProgress) {
                            if (setMetadata == false && it.metadataHash != "") {
                                zoteroDatabase.writeAttachmentInfo(
                                    AttachmentInfo(
                                        attachment.itemKey,
                                        zoteroDB.groupID,
                                        it.metadataHash,
                                        it.mtime,
                                        if (preferenceManager.isWebDAVEnabled()) {
                                            AttachmentInfo.WEBDAV
                                        } else {
                                            AttachmentInfo.ZOTEROAPI
                                        }
                                    )
                                ).blockingAwait()
                                setMetadata = true
                            }
                        }

                        override fun onError(e: Throwable) {
                            Log.d("zotero", "got error on api download, $e")
                            status = false
                        }

                    })
            }
            downloadAllProgress(status, i, attachment)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()))
            .subscribe(object : Observer<downloadAllProgress> {
                var localAttachmentSize = 0L
                var nLocalAttachments = 0

                override fun onComplete() {
                    calculateMetaInformation()
                    isDownloading = false
                    presenter.finishLoadingAnimation()
                    presenter.createErrorAlert(
                        "Finished Downloading",
                        "All your attachments have been downloaded.",
                        {})
                }

                override fun onSubscribe(d: Disposable) {
                    presenter.updateProgress("Starting Download", 0, toDownload.size)
                    downloadDisposable = d
                }

                override fun onNext(progress: downloadAllProgress) {
                    Log.d(
                        "zotero",
                        "got progress, status= ${progress.status} item=${progress.attachment.itemKey} disposeState= ${downloadDisposable?.isDisposed}"
                    )
                    if (progress.status) {
                        localAttachmentSize += attachmentStorageManager.getFileSize(
                            progress.attachment
                        )
                        nLocalAttachments++
                        presenter.displayAttachmentInformation(
                            nLocalAttachments,
                            localAttachmentSize,
                            toDownload.size
                        )

                        presenter.updateProgress(
                            progress.attachment.data["filename"] ?: "unknown",
                            progress.currentIndex,
                            toDownload.size
                        )
                    } else {
                        presenter.makeToastAlert("Error downloading ${progress.attachment.getTitle()}")
                        presenter.updateProgress("", progress.currentIndex, toDownload.size)
                    }
                }

                override fun onError(e: Throwable) {
                    presenter.createErrorAlert(
                        "Error downloading attachments",
                        "The following error occurred: ${e}",
                        {})
                }

            })
    }

    override fun loadLibrary() {
        zoteroDB = ZoteroDB(context, groupID = GroupInfo.NO_GROUP_ID)
        zoteroDB.collections = LinkedList()
        zoteroDB.loadItemsFromDatabase()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onComplete() {
                    calculateMetaInformation()
                    presenter.finishLoadingAnimation()
                }

                override fun onSubscribe(d: Disposable) {
                    presenter.displayLoadingAnimation()
                }

                override fun onError(e: Throwable) {
                    presenter.createErrorAlert(
                        "Error loading items",
                        "There was an error loading your items. " +
                                "Please verify that the library has synced fully first. Message: ${e.message}",
                        { presenter.displayErrorState() }
                    )
                }

            })
    }

    data class FilesystemMetadataObject(
        val exists: Boolean,
        val size: Long
    )


    fun stopCalculateMetaInformation() {
        calculateMetadataDisposable?.dispose()
    }

    var calculateMetadataDisposable: Disposable? = null
    fun calculateMetaInformation() {
        /* This method scans the local storage and determines what has already been downloaded on the device. */

        val attachmentItems =
            zoteroDB.items!!.filter { it.itemType == "attachment" && it.isDownloadable() }

        val observable = Observable.fromIterable(attachmentItems).map {
            Log.d("zotero", "checking if ${it.data["filename"]} exists")
            if (!it.isDownloadable() || it.data["linkMode"] == "linked_file") {
                FilesystemMetadataObject(false, -1)
            } else {
                val exists = attachmentStorageManager.checkIfAttachmentExists(it, false)
                val size = if (exists) {
                    attachmentStorageManager.getFileSize(it)
                } else {
                    -1
                }
                FilesystemMetadataObject(exists, size)
            }
        }
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<FilesystemMetadataObject> {
                var totalSize: Long = 0
                var nAttachments = 0

                override fun onSubscribe(d: Disposable) {
                    presenter.displayLoadingAnimation()
                    calculateMetadataDisposable = d
                }

                override fun onNext(metadata: FilesystemMetadataObject) {
                    if (metadata.exists) {
                        nAttachments++
                        totalSize += metadata.size
                    }
                    presenter.displayAttachmentInformation(
                        nAttachments,
                        totalSize,
                        attachmentItems.size
                    )
                }

                override fun onError(e: Throwable) {
                    presenter.createErrorAlert("Error reading filesystem", e.toString(), {})
                    Log.e("zotero", e.stackTraceToString())
                    presenter.finishLoadingAnimation()
                }

                override fun onComplete() {
                    presenter.finishLoadingAnimation()
                }

            })


    }
}