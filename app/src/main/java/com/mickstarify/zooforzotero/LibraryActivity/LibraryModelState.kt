package com.mickstarify.zooforzotero.LibraryActivity

import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo

/* This class holds information pertaining to the state of the model.
* This will allow easy reversion of the model state (so users can go back, etc)*/

class LibraryModelState {
    var currentCollection: String = "unset"
    var currentGroup: GroupInfo =
        GroupInfo(GroupInfo.NO_GROUP_ID, -1, "", "", "", "", "", "", "", -1)
    var filterText: String = ""

    fun isUsingGroup(): Boolean {
        return currentGroup.id != GroupInfo.NO_GROUP_ID

    }
}