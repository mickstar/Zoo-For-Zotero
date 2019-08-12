package com.mickstarify.zooforzotero.ZoteroAPI.Model

import com.google.gson.Gson
import org.junit.Before

import org.junit.Assert.*
import org.junit.Test

class CollectionTest {

    @Before
    fun setUp() {
    }

    @Test
    fun testJson(){
        val gson = Gson()

        val collection1 = Collection("435435", 232, CollectionData(
            "testCollection",
            "43434"))

        val s = gson.toJson(collection1)
        println(s)
        val bc1 : Collection = gson.fromJson(s, collection1.javaClass)
        assertTrue(bc1.getName() == "testCollection")
        assertTrue(bc1.getParent() == "43434")
    }
}