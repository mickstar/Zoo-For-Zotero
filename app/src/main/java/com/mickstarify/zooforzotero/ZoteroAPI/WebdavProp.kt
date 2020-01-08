package com.mickstarify.zooforzotero.ZoteroAPI

import android.util.Log

class WebdavProp {
    var mtime : Long = -1
    var hash : String

    constructor(string: String) {
        /* For use with deserializing */

        val mtimeRegex = "[.]*<mtime>([0-9]+)</mtime>[.]*".toRegex()
        val mtime = mtimeRegex.find(string)
        if (mtime == null){
            throw Exception("mtime not found in prop string.")
        }
        this.mtime = mtime.groupValues[1].toLong()

        val hashRegex = "[.]*<hash>([A-Za-z0-9_]*)</hash>[.]*".toRegex()
        val hashMatch = hashRegex.find(string)
        if (hashMatch == null){
            throw Exception("hash not found in prop string.")
        }
        this.hash = hashMatch.groupValues[1]
    }

    constructor(mtime: Long, hash: String){
        this.mtime = mtime
        this.hash = hash
    }

    fun serialize(): String {
        return """<properties version="1"><mtime>$mtime</mtime><hash>$hash</hash></properties>"""
    }
}