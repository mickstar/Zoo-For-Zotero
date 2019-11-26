package com.mickstarify.zooforzotero.ZoteroAPI

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZoteroAPITest {

    lateinit var zoteroAPI : ZoteroAPI;
    @Before
    fun setUp() {
        zoteroAPI = ZoteroAPI("QcWNZkKsdASUw4ncExhoErIp"
            ,"5884121",
            "testacc1"
        )
    }

    @Test
    fun testConnection() {
        zoteroAPI.testConnection {
                status, _ ->
            assertTrue(status)
        }
    }

    @Test
    fun testGetUploadAuthorization() {

    }
}