package com.mickstarify.zooforzotero.ZoteroAPI

import org.junit.Assert.*
import org.junit.Test

class WebdavPropTest {
    val text = "<properties version=\"1\"><mtime>1574664658000</mtime><hash>334e3d783823841e2d988257cc4b6ee2</hash></properties>"

    @Test
    fun deserializer(){
        val prop = WebdavProp(text)
        assertTrue(prop.mtime == 1574664658000L)
        assertTrue(prop.hash == "334e3d783823841e2d988257cc4b6ee2")
    }

    @Test
    fun serialize(){
        val prop = WebdavProp(1574664658000L, "334e3d783823841e2d988257cc4b6ee2")
        assertTrue(prop.mtime == 1574664658000L)
        assertTrue(prop.hash == "334e3d783823841e2d988257cc4b6ee2")
        assertTrue(prop.serialize() == text)
    }

}