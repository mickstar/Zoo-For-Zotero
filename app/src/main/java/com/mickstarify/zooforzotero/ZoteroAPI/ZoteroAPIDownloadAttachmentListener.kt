package com.mickstarify.zooforzotero.ZoteroAPI

import java.io.File
import java.util.concurrent.Future

interface ZoteroAPIDownloadAttachmentListener {
    fun onProgressUpdate(progress: Long, total: Long)
    fun onNetworkFailure()
    fun onComplete(attachment: File)
    fun onFailure()
    fun receiveTask(task: Future<Unit>)
}