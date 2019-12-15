package com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB

import android.content.Context
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase

/* This class manages multiple databases for different groups that the user is part of.
*  We will do this buy holding many zoteroDB instances. */
class ZoteroGroupDB(val context: Context, val zoteroDatabase: ZoteroDatabase) {
    private val groups: MutableMap<Int, ZoteroDB> = HashMap()

    fun getGroup(groupID: Int): ZoteroDB {
        if (groups.keys.contains(groupID)) {
            return groups[groupID]!!
        }
        val zoteroDB = ZoteroDB(
            context,
            zoteroDatabase,
            groupID
        )
        groups[groupID] = zoteroDB
        return zoteroDB
    }
}