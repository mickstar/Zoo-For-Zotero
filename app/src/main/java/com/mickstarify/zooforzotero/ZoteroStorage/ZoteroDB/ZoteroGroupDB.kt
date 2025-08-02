package com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB

import android.content.Context
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase
import com.mickstarify.zooforzotero.di.SingletonComponentsEntryPoint
import dagger.hilt.android.EntryPointAccessors

/* This class manages multiple databases for different groups that the user is part of.
*  We will do this buy holding many zoteroDB instances. */
class ZoteroGroupDB(val context: Context) {
    private val groups: MutableMap<Int, ZoteroDB> = HashMap()
    private val zoteroDatabase: ZoteroDatabase
    init {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(context, SingletonComponentsEntryPoint::class.java)
        zoteroDatabase = hiltEntryPoint.getZoteroDatabase()
    }
    fun getGroup(groupID: Int): ZoteroDB {
        if (groups.keys.contains(groupID)) {
            return groups[groupID]!!
        }
        val zoteroDB = ZoteroDB(
            context,
            groupID
        )
        groups[groupID] = zoteroDB
        return zoteroDB
    }
}