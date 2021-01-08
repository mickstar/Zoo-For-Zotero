package com.mickstarify.zooforzotero.ZoteroStorage

import android.net.Uri
import android.util.Log

// just an attempt to speed up IO but directly accessing files rather than traversing the path with DocumentFile.
// seem to only work in certain situations so this is not production.
class SAFHelper (val documentTreeLocation: String){

    fun getUriForItem(itemKey: String, filename: String): Uri {
        val regex = """content://(.+)/tree/(.+)%2F(.+)""".toRegex()
        val (location,key,name) = regex.find(documentTreeLocation)!!.destructured

        val docSubString = "/document/${key}%3A$name"

        val uriForKey = "${documentTreeLocation}${docSubString}%2F${itemKey}%2F${Uri.encode(filename)}"

        return Uri.parse(uriForKey)
    }

}