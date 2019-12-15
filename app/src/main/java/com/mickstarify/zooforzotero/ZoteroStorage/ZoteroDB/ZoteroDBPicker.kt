package com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB

import com.mickstarify.zooforzotero.LibraryActivity.LibraryActivityModel
import kotlin.reflect.KProperty

class ZoteroDBPicker(private val zoteroDB: ZoteroDB, private val zoteroGroupDB: ZoteroGroupDB) {
    /* This class picks out the zoteroDB depending on which Group the user is in.
    * So if the user is viewing his private zotero library, it will give the zoteroDB, otherwise
    * it will use the group one. */
    public var groupId: Int = -1

    fun getZoteroDB(): ZoteroDB {
        if (groupId == -1) {
            //we're using the personal db
            return zoteroDB
        }
        return zoteroGroupDB.getGroup(groupId)
    }

    fun stopGroup() {
        groupId = -1
    }

    operator fun getValue(
        libraryActivityModel: LibraryActivityModel,
        property: KProperty<*>
    ): ZoteroDB {
        return getZoteroDB()
    }

    operator fun setValue(
        libraryActivityModel: LibraryActivityModel,
        property: KProperty<*>,
        zoteroDB: ZoteroDB
    ) {
        throw Exception("do not try to set zoteroDB.")
    }
}