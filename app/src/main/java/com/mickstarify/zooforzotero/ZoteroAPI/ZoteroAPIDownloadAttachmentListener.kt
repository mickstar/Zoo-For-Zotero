package com.mickstarify.zooforzotero.ZoteroAPI

import java.io.File

interface ZoteroAPIDownloadAttachmentListener {
    fun onProgressUpdate(progress: Long, total: Long)
    fun onNetworkFailure()
    fun onComplete(attachment: File)
    fun onFailure()
}