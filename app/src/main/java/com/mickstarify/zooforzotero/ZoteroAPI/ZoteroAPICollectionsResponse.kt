package com.mickstarify.zooforzotero.ZoteroAPI

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection

data class ZoteroAPICollectionsResponse(
    val isCached: Boolean,
    val collections: List<Collection>,
    val totalResults: Int
)