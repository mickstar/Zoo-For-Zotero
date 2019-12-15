package com.mickstarify.zooforzotero.ZoteroAPI

import android.net.Uri
import java.util.concurrent.Future

interface ZoteroAPIDownloadAttachmentListener {
    fun onProgressUpdate(progress: Long, total: Long)
    fun onNetworkFailure()
    fun onComplete()
    fun onFailure(message: String = "")
    fun receiveTask(task: Future<Unit>)
}