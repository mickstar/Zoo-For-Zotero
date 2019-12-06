package com.mickstarify.zooforzotero.ZoteroAPI

import com.mickstarify.zooforzotero.ZoteroAPI.Model.CollectionPOJO

data class ZoteroAPICollectionsResponse(
    val isCached: Boolean,
    val collections: List<CollectionPOJO>,
    val totalResults: Int
)