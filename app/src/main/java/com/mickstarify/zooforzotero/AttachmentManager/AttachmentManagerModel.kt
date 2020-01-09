package com.mickstarify.zooforzotero.AttachmentManager

import android.app.Activity
import android.content.Context
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import com.mickstarify.zooforzotero.ZooForZoteroApplication
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroAPI
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroDB
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

class AttachmentManagerModel(val presenter: Contract.Presenter, val context: Context) :
    Contract.Model {

    lateinit var zoteroAPI: ZoteroAPI
    lateinit var zoteroDB: ZoteroDB
    @Inject
    lateinit var zoteroDatabase: ZoteroDatabase
    @Inject
    lateinit var attachmentStorageManager: AttachmentStorageManager

    init {
        ((context as Activity).application as ZooForZoteroApplication).component.inject(this)
        val auth = AuthenticationStorage(context)
        if (auth.hasCredentials()) {
            zoteroAPI = ZoteroAPI(auth.getUserKey(), auth.getUserID(), auth.getUsername())
        } else {
            presenter.createErrorAlert(
                "No credentials Available",
                "There is no credentials available to connect " +
                        "to the zotero server. Please relogin to the app (clear app data if necessary)"
            ) { (context.finish()) }
        }

    }

    override fun downloadAttachments() {
        val toDownload = LinkedList<Item>()
        for (attachment in zoteroDB.items!!.filter { it.itemType == "attachment" }) {
            if (attachment.data["contentType"] != "application/pdf") {
                continue
            }
            if (!attachmentStorageManager.checkIfAttachmentExists(attachment, checkMd5 = false)) {
                toDownload.add(attachment)
            }
        }
        toDownload.forEachIndexed{ i, attachment ->
            presenter.updateProgress(attachment.data["filename"]?:"unknown", i, toDownload.size)

            zoteroAPI.downloadItem()
        }

    }

    override fun loadLibrary() {
        zoteroDB = ZoteroDB(context, groupID = GroupInfo.NO_GROUP_ID)
        zoteroDB.loadItemsFromDatabase().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onComplete() {
                    showMetaInformation()
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

    private fun showMetaInformation() {
//        /*Calculates the attachment size on disk as well as on remote server.*/
        val attachmentKeys = zoteroDB.attachmentInfo!!.keys

        val localAttachments = LinkedList<Item>()
        val allAttachments = LinkedList<Item>()
        for (attachment in zoteroDB.items!!.filter { it.itemType == "attachment" }) {
            if (attachment.data["contentType"] != "application/pdf") {
                continue
            }
            allAttachments.add(attachment)
            if (attachmentStorageManager.checkIfAttachmentExists(attachment, checkMd5 = false)) {
                localAttachments.add(attachment)
            }
        }

        val localSize = localAttachments.fold(
            0L,
            { acc, item ->
                acc + 1L + attachmentStorageManager.getFileSize(
                    item
                )
            })

        presenter.displayAttachmentInformation(
            localAttachments.size,
            localSize,
            allAttachments.size
        )
    }


}