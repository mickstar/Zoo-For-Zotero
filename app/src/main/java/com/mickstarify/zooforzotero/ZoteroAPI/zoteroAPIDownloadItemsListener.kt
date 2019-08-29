package com.mickstarify.zooforzotero.ZoteroAPI

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item

interface ZoteroAPIDownloadItemsListener {
    fun onCachedComplete()
    fun onNetworkFailure()
    fun onDownloadComplete(items: List<Item>, libraryVersion: Int)
    fun onProgressUpdate(progress: Int, total: Int)
}