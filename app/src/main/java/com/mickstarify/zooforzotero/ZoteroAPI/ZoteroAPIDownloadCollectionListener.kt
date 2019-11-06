package com.mickstarify.zooforzotero.ZoteroAPI

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection

interface ZoteroAPIDownloadCollectionListener {
    fun onCachedComplete()
    fun onNetworkFailure()
    fun onDownloadComplete(collections: List<Collection>)
    fun onProgressUpdate(progress: Int, total: Int)
}