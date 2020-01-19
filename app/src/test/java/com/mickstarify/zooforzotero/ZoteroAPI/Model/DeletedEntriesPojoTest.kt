package com.mickstarify.zooforzotero.ZoteroAPI.Model

import com.google.gson.Gson
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletedEntriesPojoTest{
    @Test
    fun testSerialize(){
        val gson = Gson()

        val o = gson.fromJson<DeletedEntriesPojo>(json, DeletedEntriesPojo::class.java)
        assertTrue(o.searches.size == 0)
        assertTrue(o.items.get(0) == "2CXCJ3FA")
        assertTrue(o.tags != null)
        assertTrue(o.collections.size == 0)
    }


    val json = """{
    "collections": [],
    "items": [
        "2CXCJ3FA",
        "88PLXW2K",
        "AXTIZ8WC",
        "CA6VV6DM",
        "DB6XW3CU",
        "DFL9PMMM",
        "F7JTW8F2",
        "GANIKZC7",
        "HHBGZT47",
        "HQTVJ66X",
        "HUQL8R9M",
        "JRXDNBHS",
        "MBVYKTJW",
        "PMJ9WW52",
        "V9J5V5SB",
        "VQMQT4TR",
        "XSC624P8",
        "XZ3HUDC6",
        "Y39QDC7S",
        "Z3EAIAE4",
        "ZH8QJWCL",
        "ZMP35IIE",
        "ZPV9KEZX"
    ],
    "searches": [],
    "tags": [],
    "settings": []
}"""
}