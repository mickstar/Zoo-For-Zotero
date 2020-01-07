package com.mickstarify.zooforzotero.ZoteroAPI

import com.mickstarify.zooforzotero.ZoteroAPI.Model.ItemPOJO

data class ZoteroAPIItemsResponse(
    val isCached: Boolean, // holds whether the server returned a 304 or not.
    // if isCached is true, the rest of this object is invalid and should be ignored.
    val items: List<ItemPOJO>,
    val LastModifiedVersion: Int,
    val totalResults: Int
)