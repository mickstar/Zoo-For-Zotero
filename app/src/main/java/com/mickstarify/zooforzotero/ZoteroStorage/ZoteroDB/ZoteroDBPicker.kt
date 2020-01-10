package com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB

import com.mickstarify.zooforzotero.LibraryActivity.LibraryActivityModel
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import javax.inject.Singleton
import kotlin.reflect.KProperty

@Singleton
class ZoteroDBPicker(private val zoteroDB: ZoteroDB, private val zoteroGroupDB: ZoteroGroupDB) {
    /* This class picks out the zoteroDB depending on which Group the user is in.
    * So if the user is viewing his private zotero library, it will give the zoteroDB, otherwise
    * it will use the group one. */
    var groupId: Int = GroupInfo.NO_GROUP_ID

    fun getZoteroDB(): ZoteroDB {
        if (groupId == GroupInfo.NO_GROUP_ID) {
            //we're using the personal db
            return zoteroDB
        }
        return zoteroGroupDB.getGroup(groupId)
    }

    fun stopGroup() {
        groupId = GroupInfo.NO_GROUP_ID
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