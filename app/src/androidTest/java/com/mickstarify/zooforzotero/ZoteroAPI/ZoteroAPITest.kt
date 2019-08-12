package com.mickstarify.zooforzotero.ZoteroAPI

import android.util.Log
import org.junit.Test

import org.junit.Before

class ZoteroAPITest {
    lateinit var zoteroAPI : ZoteroAPI;
    @Before
    fun setUp() {
        zoteroAPI = ZoteroAPI("voaRLiSlDf5Fu2kRkBsd07Ac"
            ,"5884121",
            "testacc1",0)
    }

    @Test
    fun testConnection() {
        Log.d("zotero","hello world")
        zoteroAPI.testConnection()
    }
}