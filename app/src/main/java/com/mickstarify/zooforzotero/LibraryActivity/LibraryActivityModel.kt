package com.mickstarify.zooforzotero.LibraryActivity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.google.gson.JsonObject
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import com.mickstarify.zooforzotero.ZoteroAPI.AlreadyUploadedException
import com.mickstarify.zooforzotero.ZoteroAPI.DeleteItemListener
import com.mickstarify.zooforzotero.ZoteroAPI.DownloadProgress
import com.mickstarify.zooforzotero.ZoteroAPI.ItemChangedSinceException
import com.mickstarify.zooforzotero.ZoteroAPI.ItemLockedException
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import com.mickstarify.zooforzotero.ZoteroAPI.PreconditionFailedException
import com.mickstarify.zooforzotero.ZoteroAPI.RequestEntityTooLarge
import com.mickstarify.zooforzotero.ZoteroAPI.Syncing.OnSyncChangeListener
import com.mickstarify.zooforzotero.ZoteroAPI.Syncing.SyncManager
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroAPI
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroNotFoundException
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.AttachmentInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import com.mickstarify.zooforzotero.ZoteroStorage.Database.RecentlyOpenedAttachment
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroDB
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroDBPicker
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroGroupDB
import com.mickstarify.zooforzotero.di.SingletonComponentsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.MaybeObserver
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.FileNotFoundException
import java.util.LinkedList
import java.util.Stack
import java.util.concurrent.TimeUnit
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection as ZoteroCollection


class LibraryActivityModel(private val presenter: Contract.Presenter, val context: Context) :
    Contract.Model, OnSyncChangeListener {

    // stores the current item being viewed by the user. (useful for refreshing the view)
    var selectedItem: Item? = null
    var isDisplayingItems = false

    private lateinit var zoteroAPI: ZoteroAPI

    private var syncManager: SyncManager

    val zoteroDatabase: ZoteroDatabase
    val attachmentStorageManager: AttachmentStorageManager
    val preferences: PreferenceManager

    private val zoteroGroupDB =
        ZoteroGroupDB(
            context
        )
    private var zoteroDBPicker =
        ZoteroDBPicker(
            ZoteroDB(
                context,
                groupID = -1
            ), zoteroGroupDB
        )
    private var groups: List<GroupInfo>? = null


    val states = Stack<LibraryModelState>()
    val state: LibraryModelState
        get() {
            return states.peek()
        }
    private var zoteroDB: ZoteroDB by zoteroDBPicker

    private var performedCleanSync: Boolean = false

    override fun refreshLibrary(useSmallLoadingAnimation: Boolean) {
        if (!state.isUsingGroup()) {
            downloadLibrary(doRefresh = true, useSmallLoadingAnimation = useSmallLoadingAnimation)
        } else {
            this.startGroupSync(state.currentGroup, refresh = true)
        }
    }

    fun shouldIUpdateLibrary(): Boolean {
        val currentTimestamp = System.currentTimeMillis()
        val lastModified = zoteroDB.getLastSyncedTimestamp()
        Log.d("zotero", "last modified = $lastModified")

        if (TimeUnit.MILLISECONDS.toHours(currentTimestamp - lastModified) >= 24) {
            return true
        }
        return false
    }

    override fun isLoaded(): Boolean {
        return zoteroDB.isPopulated()
    }

    private fun filterItems(items: List<Item>): List<Item> {
        val onlyNotes = preferences.getIsShowingOnlyNotes()
        val onlyPdfs = preferences.getIsShowingOnlyPdfs()

        var newItems = items
        if (onlyNotes) {
            newItems =
                newItems.filter { it.notes.size > 0 }
        }
        if (onlyPdfs) {
            newItems = newItems.filter {
                getAttachments(it.itemKey).fold(false, { acc, attachment ->
                    var result = acc
                    if (!result) {
                        result = attachment.data["contentType"] == "application/pdf"
                    }
                    result
                })
            }
        }
        return newItems
    }

    override fun getGroupByTitle(groupTitle: String): GroupInfo? {
        return this.groups?.filter { it.name == groupTitle }?.firstOrNull()
    }

    override fun downloadLibrary(doRefresh: Boolean, useSmallLoadingAnimation: Boolean) {
        if (!doRefresh && zoteroDB.isPopulated()) {
            Log.d("zotero", "not resyncing library, already have a copy.")
            return
        }
        if (zoteroDB.getLibraryVersion() <= 0) {
            this.performedCleanSync = true
        }
        syncManager.startCompleteSync(zoteroDB, useSmallLoadingAnimation)
    }

    override fun getLibraryItems(): List<Item> {
        return filterItems(zoteroDB.getDisplayableItems())
    }

    override fun getItemsFromCollection(collectionKey: String): List<Item> {
        val items = zoteroDB.getItemsFromCollection(collectionKey)
        return filterItems(items)
    }

    override fun getSubCollections(collectionKey: String): List<ZoteroCollection> {
//        val collectionKey = zoteroDB.getCollectionId(collectionName)
        return zoteroDB.getSubCollectionsFor(collectionKey)
    }


    override fun getCollections(): List<ZoteroCollection> {
        return zoteroDB.collections ?: LinkedList()
    }

    override fun getAttachments(itemKey: String): List<Item> {
        return zoteroDB.getAttachments(itemKey)
    }

    override fun getMyPublications(): List<Item> {
        return zoteroDB.myPublications ?: LinkedList()
    }

    override fun filterCollections(query: String): List<ZoteroCollection> {
        val queryUpper = query.uppercase()
        return zoteroDB.collections?.filter {
            it.name.uppercase().contains(queryUpper)
        } ?: LinkedList()
    }

    override fun openPDF(attachment: Item) {
        /* This method creates an intent to open a PDF for Android. */

        Completable.fromAction {
            // used to check attachments for filechanges.
            if (preferences.isAttachmentUploadingEnabled()) {
                zoteroDatabase.addRecentlyOpenedAttachments(
                    RecentlyOpenedAttachment(
                        attachment.itemKey,
                        attachment.getVersion()
                    )
                )
                    .blockingAwait()
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnComplete {
            try {
                val intent = attachmentStorageManager.openAttachment(attachment)
                context.startActivity(intent)
            } catch (exception: ActivityNotFoundException) {
                presenter.createErrorAlert("No PDF Viewer installed",
                    "There is no app that handles ${attachment.getFileExtension()} documents available on your device. Would you like to install one?",
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=com.xodo.pdf.reader")
                                )
                            )
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=com.xodo.pdf.reader")
                                )
                            )
                        }
                    })
            } catch (e: IllegalArgumentException) {
                presenter.createErrorAlert("Error opening File",
                    "There was an error opening your PDF." +
                            " If you are on a huawei device, this is a known error with their implementation of file" +
                            " access. Try changing the storage location to a custom path in settings.",
                    {})
            } catch (e: FileNotFoundException) {
                presenter.createErrorAlert("File not found",
                    "There was an error opening your PDF." +
                            "Please redownload your file.",
                    {})
            }
        }.doOnError {
            if (it is FileNotFoundException) {
                Log.d("zotero", "file not found")
            } else {
                throw it
            }
        }.subscribe()
    }

    override fun openAttachment(item: Item) {
        /* This is the point of entry when a user clicks an attachment on the UI.
        *  We must decide whether we want to intitiate a download or just open a local copy. */

        // first check to see if we are opening a linked attachment
        if (item.data["linkMode"] == "linked_file") {
            val intent = attachmentStorageManager.openLinkedAttachment(item)
            if (intent != null) {
                context.startActivity(intent)
            } else {
                presenter.makeToastAlert("Error, could not find linked attachment ${item.data["path"]}")
            }
            return
        }
        presenter.showLoadingAlertDialog("Opening your attachment")
        // check to see if the attachment exists but is invalid
        val attachmentExists: Boolean
        try {
            attachmentExists = attachmentStorageManager.checkIfAttachmentExists(
                item,
                checkMd5 = false
            )
        } catch (e: Exception) {
            presenter.makeToastAlert("could not open attachment, file not found.")
            return
        }
        if (attachmentExists) {
            // check the validity of the attachment before opening.
            presenter.showLoadingAlertDialog("Verifying MD5 Checksum")
            if (preferences.shouldCheckMd5SumBeforeOpening() && zoteroDB.hasMd5Key(
                    item,
                    onlyWebdav = preferences.isWebDAVEnabled()
                ) && !attachmentStorageManager.validateMd5ForItem(
                    item,
                    zoteroDB.getMd5Key(item)
                )
            ) {
                presenter.createYesNoPrompt(
                    "File conflict",
                    "Your local copy is different to the server's. Would you like to redownload the server's copy?",
                    "Yes", "No", {
                        presenter.updateAttachmentDownloadProgress(0, -1)
                        attachmentStorageManager.deleteAttachment(item)
                        downloadAttachment(item)
                    }, {
                        presenter.hideLoadingAlertDialog()
                        openPDF(item)
                    }
                )
                return
            } else {
                presenter.hideLoadingAlertDialog()
                openPDF(item)
            }

        } else {
            presenter.hideLoadingAlertDialog()
            presenter.updateAttachmentDownloadProgress(0, -1)
            downloadAttachment(item)
        }

    }

    override fun filterItems(query: String): List<Item> {
        val items = zoteroDB.items?.filter {
            it.query(query)

        } ?: LinkedList<Item>()

        return items
    }

    var downloadDisposable: Disposable? = null

    override fun downloadAttachment(item: Item) {
        if (isDownloading) {
            Log.d("zotero", "not downloading ${item.getTitle()} because i am already downloading.")
            return
        }
        isDownloading = true

        zoteroAPI.downloadItemRx(item, state.currentGroup.id, context)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<DownloadProgress> {
                var receivedMetadata = false

                override fun onComplete() {
                    isDownloading = false
                    presenter.finishDownloadingAttachment()

                    if (zoteroDB.hasMd5Key(item) && !attachmentStorageManager.validateMd5ForItem(
                            item, zoteroDB.getMd5Key(item)
                        )
                    ) {
                        Log.d("zotero", "md5 error on attachment ${zoteroDB.getMd5Key(item)}")
                        presenter.createErrorAlert(
                            "MD5 Verification Error",
                            "The downloaded file does not match the accompanying md5 checksum.",
                            {})
                        attachmentStorageManager.deleteAttachment(item)
                        return
                    } else {
                        openPDF(item)
                    }
                }

                override fun onSubscribe(d: Disposable) {
                    downloadDisposable = d

                }

                override fun onNext(t: DownloadProgress) {
                    presenter.updateAttachmentDownloadProgress(t.progress, t.total)
                    if (!receivedMetadata && t.metadataHash != "") {
                        receivedMetadata = true
                        zoteroDB.updateAttachmentMetadata(
                            item.itemKey,
                            t.metadataHash,
                            t.mtime,
                            if (preferences.isWebDAVEnabled()) {
                                AttachmentInfo.WEBDAV
                            } else {
                                AttachmentInfo.ZOTEROAPI
                            }
                        ).subscribeOn(Schedulers.io()).subscribe()
                    }
                }

                override fun onError(e: Throwable) {
                    Log.e("zotero", "got error ${e}")
//                    if (downloadDisposable?.isDisposed == true) {
//                        return
//                    }

                    if (e is ZoteroNotFoundException) {
                        presenter.attachmentDownloadError(
                            "The file does not exist on the Zotero server."
                        )
                    } else {
                        presenter.attachmentDownloadError(
                            "Error Message: ${e.message}"
                        )
                    }
                    isDownloading = false
                }

            })
    }


    override fun cancelAttachmentDownload() {
        Log.d("zotero", "cancelling download")
        this.isDownloading = false
        downloadDisposable?.dispose()
    }

    var isDownloading: Boolean = false
    var currentlyDownloadingAttachment: Item? = null

    override fun getUnfiledItems(): List<Item> {
        if (!zoteroDB.isPopulated()) {
            Log.e("zotero", "error zoteroDB not populated!")
            return LinkedList()
        }
        return filterItems(zoteroDB.getItemsWithoutCollection())
    }

    override fun createNote(note: Note) {
        if (state.isUsingGroup()) {
            presenter.makeToastAlert("Sorry, this isn't supported in shared collections.")
            return
        }
        zoteroAPI.uploadNote(note)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onComplete() {
                    presenter.hideBasicSyncAnimation()
                    syncManager.startItemsSync(db = zoteroDB)
                    presenter.makeToastAlert("Successfully created your note")
                }

                override fun onSubscribe(d: Disposable) {
                    presenter.showBasicSyncAnimation()
                }

                override fun onError(e: Throwable) {
                    presenter.createErrorAlert(
                        "Error creating note",
                        "An error occurred while trying to create your note. Message: $e"
                    ) {}
                }
            })


    }

    override fun modifyNote(note: Note) {
        if (state.isUsingGroup()) {
            presenter.makeToastAlert("Sorry, this isn't supported in shared collections.")
            return
        }
        zoteroAPI.modifyNote(note, zoteroDB.getLibraryVersion())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onComplete() {
                    presenter.hideBasicSyncAnimation()
                    syncManager.startItemsSync(db = zoteroDB)
                    presenter.makeToastAlert("Successfully modified your note")
                }

                override fun onSubscribe(d: Disposable) {
                    presenter.showBasicSyncAnimation()
                }

                override fun onError(e: Throwable) {
                    if (e is ItemLockedException) {
                        presenter.createErrorAlert(
                            "Error modifying note",
                            "The note you are editing has been locked or you do not have permission to change it."
                        ) {}
                    } else if (e is ItemChangedSinceException) {
                        presenter.createErrorAlert(
                            "Error modifying note",
                            "The version on Zotero's servers is newer than your local copy. Please refresh your library."
                        ) {}

                    } else {
                        presenter.createErrorAlert(
                            "Error modifying note",
                            "An error occurred while trying to create your note. Message: $e"
                        ) {}
                    }
                    presenter.hideBasicSyncAnimation()
                }
            })
    }

    override fun deleteNote(note: Note) {
        if (state.isUsingGroup()) {
            presenter.makeToastAlert("Sorry, this isn't supported in shared collections.")
            return
        }
        zoteroAPI.deleteItem(note.key, note.version, object : DeleteItemListener {
            override fun success() {
                presenter.makeToastAlert("Successfully deleted your note.")
                zoteroDB.deleteItem(note.key)
                presenter.refreshItemView()
            }

            override fun failedItemLocked() {
                presenter.createErrorAlert(
                    "Error Deleting Note", "The item is locked " +
                            "and you do not have permission to delete this note."
                ) {}
            }

            override fun failedItemChangedSince() {
                presenter.createErrorAlert(
                    "Error Deleting Note",
                    "Your local copy of this note is out of date. " +
                            "Please refresh your library to delete this note."
                ) {}
            }

            override fun failed(code: Int) {
                presenter.createErrorAlert(
                    "Error Deleting Note", "There was an error " +
                            "deleting your note. The server responded : $code"
                ) {}
            }
        })
    }

    override fun deleteAttachment(item: Item) {
        zoteroAPI.deleteItem(item.itemKey, item.getVersion(), object : DeleteItemListener {
            override fun success() {
                presenter.makeToastAlert("Successfully deleted your attachment.")
                zoteroDB.deleteItem(item.itemKey)
                presenter.refreshItemView()
            }

            override fun failedItemLocked() {
                presenter.createErrorAlert(
                    "Error Deleting Attachment", "The item is locked " +
                            "and you do not have permission to delete this attachment."
                ) {}
            }

            override fun failedItemChangedSince() {
                presenter.createErrorAlert(
                    "Error Deleting Attachment",
                    "Your local copy of this note is out of date. " +
                            "Please refresh your library to delete this attachment."
                ) {}
            }

            override fun failed(code: Int) {
                presenter.createErrorAlert(
                    "Error Deleting Attachment", "There was an error " +
                            "deleting your attachment. The server responded : $code"
                ) {}
            }
        })
    }

    fun displayGroupsOnUI() {
        val groups = zoteroDatabase.getGroups()
        groups.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : MaybeObserver<List<GroupInfo>> {
                override fun onSuccess(groupInfo: List<GroupInfo>) {
                    Log.d("zotero", "completed get group info.")
                    this@LibraryActivityModel.groups = groupInfo
                    presenter.displayGroupsOnActionBar(groupInfo)
                }

                override fun onComplete() {
                    Log.d("zotero", "User has no groups.")
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {
                    Log.e("zotero", "error loading groups.")
                    presenter.createErrorAlert(
                        "Error loading group data",
                        "Message: ${e.message}",
                        {})
                }

            })
    }

    fun loadGroups() {
        /* This method loads a list of groups. It does not deal with getting items or catalogs. */

        val groupsObserver = zoteroAPI.getGroupInfo()
        groupsObserver.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .subscribe(object : Observer<List<GroupInfo>> {
                override fun onComplete() {
                    Log.d("zotero", "completed getting group info")
                    displayGroupsOnUI()
                }

                override fun onSubscribe(d: Disposable) {
                    Log.d("zotero", "subscribed to group info")
                }

                override fun onNext(groupInfo: List<GroupInfo>) {
                    groupInfo.forEach {
                        val status = zoteroDatabase.addGroup(it)
                        status.blockingAwait() // wait until the db add is finished.
                    }
                }

                override fun onError(e: Throwable) {
                }

            })
    }

    fun checkAllAttachmentsForModification() {
        if (state.isUsingGroup()) {
            Log.d(
                "zotero",
                "not checking attachments because we do not support groups"
            )
            return
        }

        if (!preferences.isAttachmentUploadingEnabled()) {
            Log.d("zotero", "Not checking attachments, disabled by preferences")
            return
        }

        zoteroDatabase.getRecentlyOpenedAttachments()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).map { listOfRecently ->
                val itemsToUpload: MutableList<Pair<Item, Int>> = LinkedList()
                for (recentlyOpenedAttachment in listOfRecently) {
                    Log.d("zotero", "RECENTLY OPENED ${recentlyOpenedAttachment.itemKey}")
                    val item = zoteroDB.getItemWithKey(recentlyOpenedAttachment.itemKey)
                    if (item != null) {
                        try {
                            val md5Key = zoteroDB.getMd5Key(item)
                            if (md5Key != "" && attachmentStorageManager.validateMd5ForItem(
                                    item,
                                    md5Key
                                ) == false
                            ) {
                                // our attachment has been modified, we will offer to upload.
                                itemsToUpload.add(Pair(item, recentlyOpenedAttachment.version))
                                Log.d(
                                    "zotero",
                                    "found change in ${item.getTitle()} ${item.itemKey}"
                                )
                            } else {
                                // the item hasnt changed.
                                removeFromRecentlyViewed(item)
                            }
                        } catch (e: FileNotFoundException) {
                            Log.d(
                                "zotero",
                                "could not find local attachment with itemKey ${item.itemKey}"
                            )
                            removeFromRecentlyViewed(item)
                        } catch (e: Exception) {
                            Log.e("zotero", "validateMd5 got error $e")
                            removeFromRecentlyViewed(item)
                        }
                    }
                }
                itemsToUpload
            }.subscribe(object : MaybeObserver<MutableList<Pair<Item, Int>>> {
                override fun onComplete() {
                    // do nothing
                }

                override fun onSubscribe(d: Disposable) {
                    // do nothing
                }


                override fun onError(e: Throwable) {
                    Log.e("zotero", "validateMd5 observer got error $e")
                }

                override fun onSuccess(itemsToUpload: MutableList<Pair<Item, Int>>) {
                    if (itemsToUpload.isNotEmpty()) {
                        askToUploadAttachments(itemsToUpload)
                    }
                }

            })
    }

    fun askToUploadAttachments(changedAttachments: List<Pair<Item, Int>>) {
        // for the sake of sanity I will only ask to upload 1 attachment.
        // this is because of limitations of only having 1 upload occur concurrently
        // and my unwillingness to implement a chaining mechanism for uploads for what i expect to
        // be a niche power user.
        val attachment = changedAttachments.first().first
        val version = changedAttachments.first().second
        val fileSizeBytes = attachmentStorageManager.getFileSize(
            attachmentStorageManager.getAttachmentUri(attachment)
        )

        if (fileSizeBytes == 0L) {
            Log.e("zotero", "avoiding uploading a garbage PDF")
            attachmentStorageManager.deleteAttachment(attachment)
            removeFromRecentlyViewed(attachment)
            return
        }

        val sizeKiloBytes = "${fileSizeBytes / 1000}KB"

        val message =
            "${attachment.data["filename"]!!} ($sizeKiloBytes) is different to Zotero's version. Would you like to upload this PDF to replace the remote version?"

        presenter.createYesNoPrompt(
            "Detected changes to attachment",
            message,
            "Upload",
            "No",
            {
                if (version < attachment.getVersion()) {
                    presenter.createYesNoPrompt("Outdated Version",
                        "This local copy is older than the version on Zotero's server, are you sure you upload (this will irreversibly overwrite the server's copy)",
                        "I am sure",
                        "Cancel",
                        { uploadAttachment(attachment) },
                        { removeFromRecentlyViewed(attachment) })
                }
                uploadAttachment(attachment)
            },
            { removeFromRecentlyViewed(attachment) })
    }

    override fun uploadAttachment(attachment: Item) {
        val md5Key: String
        try {
            md5Key = attachmentStorageManager.calculateMd5(attachment)
        } catch (e: FileNotFoundException) {
            presenter.makeToastAlert("Cannot upload attachment. File does not exist.")
            return
        }
        var mtime = attachmentStorageManager.getMtime(attachment)

        if (mtime < attachment.getMtime()) {
            Log.e("zotero", "for some reason our mtime is older than the original???")
            mtime = attachment.getMtime()
        }
        if (preferences.isWebDAVEnabled()) {
            zoteroAPI.uploadAttachmentWithWebdav(attachment, context).andThen(
                Completable.fromAction {
                    // TODO extract this complexity to some other class.
                    if (preferences.isWebDAVEnabled()) {
                        val modificationJsonObject = JsonObject()
                        modificationJsonObject.addProperty("md5", md5Key)
                        modificationJsonObject.addProperty("mtime", mtime)
                        zoteroAPI.patchItem(attachment, modificationJsonObject).blockingAwait()
                    }
                }
            )
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                    object : CompletableObserver {
                        override fun onComplete() {
                            presenter.stopUploadingAttachmentProgress()
                            removeFromRecentlyViewed(attachment)
                            zoteroDB.updateAttachmentMetadata(
                                attachment.itemKey,
                                attachmentStorageManager.calculateMd5(attachment),
                                attachmentStorageManager.getMtime(attachment),
                                AttachmentInfo.WEBDAV
                            ).subscribeOn(Schedulers.io()).subscribe()
                        }

                        override fun onSubscribe(d: Disposable) {
                            presenter.startUploadingAttachmentProgress(attachment)
                        }

                        override fun onError(e: Throwable) {
                            presenter.createErrorAlert(
                                "Error uploading Attachment",
                                e.toString(),
                                {})
                            Log.e("zotero", "got exception: $e")
                            presenter.stopUploadingAttachmentProgress()
                        }
                    })
            return
        } else {
            zoteroAPI.updateAttachment(attachment)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object :
                    CompletableObserver {
                    override fun onComplete() {
                        presenter.stopUploadingAttachmentProgress()
                        removeFromRecentlyViewed(attachment)
                        zoteroDB.updateAttachmentMetadata(
                            attachment.itemKey,
                            attachmentStorageManager.calculateMd5(attachment),
                            attachmentStorageManager.getMtime(attachment),
                            AttachmentInfo.ZOTEROAPI
                        ).subscribeOn(Schedulers.io()).subscribe()
                    }

                    override fun onSubscribe(d: Disposable) {
                        presenter.startUploadingAttachmentProgress(attachment)
                    }

                    override fun onError(e: Throwable) {
                        if (e is AlreadyUploadedException) {
                            removeFromRecentlyViewed(attachment)
                            zoteroDB.updateAttachmentMetadata(
                                attachment.itemKey,
                                attachmentStorageManager.calculateMd5(attachment),
                                attachmentStorageManager.getMtime(attachment),
                                AttachmentInfo.WEBDAV
                            ).subscribeOn(Schedulers.io()).subscribe()
                            presenter.makeToastAlert("Attachment already up to date.")
                        } else if (e is PreconditionFailedException) {
                            presenter.createErrorAlert(
                                "Error uploading Attachment",
                                "The server's copy of this attachment is newer than yours. " +
                                        "Please sync your library again to get the up to date version. " +
                                        "You will need to back up your annotated file first if you wish to keep this changes.",
                                {})
                        } else if (e is RequestEntityTooLarge) {
                            presenter.createErrorAlert(
                                "Error uploading Attachment",
                                "You do not have enough storage quota to store this atttachment.",
                                {})
                        } else {
                            presenter.createErrorAlert(
                                "Error uploading Attachment",
                                e.toString(),
                                {})
                            Log.e("zotero", "got exception: $e")
                        }
                        presenter.stopUploadingAttachmentProgress()
                    }

                })
        }
    }

    override fun removeFromRecentlyViewed(attachment: Item) {
        zoteroDatabase.deleteRecentlyOpenedAttachment(attachment.itemKey)
            .subscribeOn(Schedulers.io()).subscribe()
    }

    override fun startGroupSync(group: GroupInfo, refresh: Boolean) {
        if (syncManager.isSyncing()) {
            Log.e("zotero", "cannot sync groups, still in a sync")
            presenter.makeToastAlert("Cannot open groups yet, still syncing library.")
            return
        }

        val db = zoteroGroupDB.getGroup(group.id)
        if (!refresh && db.isPopulated()) {
            // already synced and loaded.
            finishLibrarySync(db)
            return
        }

        syncManager.startCompleteSync(db, false)
    }

    override fun usePersonalLibrary() {
        this.zoteroDBPicker.stopGroup()
    }

    fun getCurrentCollection(): String {
        return state.currentCollection
    }

    fun setCurrentCollection(collectionName: String, usePersonalLibrary: Boolean = true) {
        // check to see if we're in a new state first.
        if (state.currentCollection == collectionName) {
            return
        }

        val newState = LibraryModelState()
        if (!usePersonalLibrary) {
            newState.currentGroup = state.currentGroup
        }
        newState.currentCollection = collectionName
        newState.filterText = state.filterText
        states.push(newState)
    }

    fun checkAttachmentStorageAccess() {
        try {
            if (attachmentStorageManager.testStorage() == false) {
                throw Exception()
            }
        } catch (e: Exception) {
            Log.e("zotero", "error testing storage. ${e}")
            presenter.createErrorAlert(
                "Permission Error",
                "There was an error accessing your zotero attachment location. Please reconfigure in settings.",
                {})
            preferences.useExternalCache()
        }
    }

    fun getTrashedItems(): List<Item> {
        return zoteroDB.getTrashItems()
    }

    fun getCollectionFromKey(collectionKey: String): ZoteroCollection? {
        return zoteroDB.getCollectionById(collectionKey)
    }

    fun destroyLibrary() {
        zoteroDB.clearItemsVersion()
        zoteroDatabase.deleteEverything()
    }

    fun isUsingGroups(): Boolean {
        return state.isUsingGroup()
    }

    fun getCurrentGroup(): GroupInfo? {
        return state.currentGroup
    }

    fun loadLibraryLocally(onFinish: () -> Unit = {}) {
        if (!zoteroDB.isPopulated())
            finishLibrarySync(zoteroDB) {
                onFinish()
            }
    }

    override fun isFirstSync(): Boolean {
        return zoteroDB.getLastSyncedTimestamp() == 0L
    }

    override fun finishLibrarySync(db: ZoteroDB, onFinish: () -> Unit) {
        /* This method is the endpoint for zotero api syncs,
        The role of this function is to load the library into memory and then
        trigger a UI update.
        * This will be called from the SyncManager. */
        Log.d("zotero", "finished library loading.")


        val loadCollections = db.loadCollectionsFromDatabase().doOnError { e ->
            Log.e("zotero", "loading collections from db got error $e")
            db.collections = LinkedList()
        }

        val loadItems = db.loadItemsFromDatabase().doOnError { e ->
            Log.e("zotero", "loading Items from db got error $e")
            db.items = LinkedList()
        }

        loadCollections
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .andThen(loadItems)
            .andThen(db.loadTrashItemsFromDB())
            .doOnComplete {
                presenter.hideBasicSyncAnimation()
                if (db.groupID == GroupInfo.NO_GROUP_ID) {
                    presenter.receiveCollections(getCollections())
                    if (state.currentCollection == "unset") {
                        presenter.setCollection("all")
                    } else {
                        presenter.redisplayItems()
                    }
                } else {
                    zoteroDBPicker.groupId = db.groupID
                    val newState = LibraryModelState().apply {
                        this.currentCollection = "group_all"
                        this.currentGroup = groups?.filter { it.id == db.groupID }?.firstOrNull()!!
                    }
                    this.states.add(newState)
                    presenter.redisplayItems()
                }
                if (db.items?.size == 0) {
                    // incase there was an error, i don't want users to be stuck with an empty library.
                    db.setItemsVersion(0)
                }
                this.checkAllAttachmentsForModification()
            }.onErrorComplete {
                presenter.createErrorAlert("error loading library", "got error message ${it}", {})
                presenter.hideBasicSyncAnimation()
                true
            }
            .doAfterTerminate {
                onFinish()
            }.subscribe()
    }

    private var doubleBackToExitPressedOnce = false
    override fun loadPriorState() {
        // check to see if we're at the root level, the first state is a junk state, so we
        // check at 2.
        if (states.size <= 2) {
            if (doubleBackToExitPressedOnce) {
                (this.context as Activity).finish()
                return
            }

            this.doubleBackToExitPressedOnce = true
            presenter.makeToastAlert("Please press BACK again to exit")
            Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
            return
        }
        val oldState = states.peek()
        states.pop()
        if (oldState.currentGroup != state.currentGroup) {
            if (state.isUsingGroup()) {
                presenter.openGroup(state.currentGroup.name)
            } else {
                // we are returning to the user's collection.
                this.usePersonalLibrary()
                presenter.setCollection(state.currentCollection)
            }
        } else if (oldState.currentCollection != state.currentCollection) {
            presenter.setCollection(state.currentCollection)
        } else if (oldState.filterText != state.filterText) {
            if (state.filterText == "") {
                presenter.closeQuery()
            } else {
                presenter.filterEntries(state.filterText)
            }
        } else {
            Log.e("zotero", "error unable to determine state differences!!")
        }
    }

    override fun getItemsForTag(tagName: String): List<Item> {
        return zoteroDB.getItemsForTag(tagName)
    }

    override fun deleteLocalAttachment(attachment: Item) {
        attachmentStorageManager.deleteAttachment(attachment)
    }

    init {
        val appContext = (context as Activity).applicationContext
        val hiltEntryPoint = EntryPointAccessors.fromApplication(appContext, SingletonComponentsEntryPoint::class.java)

        zoteroDatabase = hiltEntryPoint.getZoteroDatabase()
        attachmentStorageManager = hiltEntryPoint.getAttachmentStorageManager()
        preferences = hiltEntryPoint.getPreferences()

        val auth = AuthenticationStorage(context)
        // add the first library state.
        states.push(LibraryModelState())

        if (auth.hasCredentials()) {
            zoteroAPI = ZoteroAPI(
                auth.getUserKey(),
                auth.getUserID(),
                auth.getUsername(),
                attachmentStorageManager,
                preferences
            )
        } else {
            presenter.createErrorAlert(
                "Error with stored API",
                "The API Key we have stored in the application is invalid!" +
                        "Please re-authenticate the application"
            ) { context.finish() }
            auth.destroyCredentials()
            zoteroDB.clearItemsVersion()
        }

        syncManager = SyncManager(zoteroAPI, preferences, zoteroDatabase, this)
        checkAttachmentStorageAccess()
    }

    override fun startSyncAnimation(useSmallAnimation: Boolean) {
        if (useSmallAnimation) {
            presenter.showBasicSyncAnimation()
        } else {
            presenter.showLibraryLoadingAnimation()
        }
    }

    override fun stopSyncAnimation() {
        presenter.hideLibraryLoadingAnimation()
    }

    override fun createErrorAlert(title: String, message: String, onClick: () -> Unit) {
        presenter.createErrorAlert(title, message, onClick)
    }

    override fun setSyncProgress(progress: Int, total: Int) {
        presenter.updateLibraryRefreshProgress(progress, total, "")
    }

    override fun makeToastAlert(message: String) {
        presenter.makeToastAlert(message)
    }
}