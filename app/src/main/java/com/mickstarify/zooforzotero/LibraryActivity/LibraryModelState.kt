package com.mickstarify.zooforzotero.LibraryActivity

import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo

/* This class holds information pertaining to the state of the model.
* This will allow easy reversion of the model state (so users can go back, etc)*/
class LibraryModelState {
    var currentCollection: String = "unset"

    var usingGroup: Boolean = false
    var currentGroup: GroupInfo? = null
}