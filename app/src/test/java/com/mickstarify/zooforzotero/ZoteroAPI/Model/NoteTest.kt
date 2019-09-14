package com.mickstarify.zooforzotero.ZoteroAPI.Model

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NoteTest {
    var note: Note? = null
    @Before
    fun setUp() {
        val items = ItemJSONConverter().deserialize(json)
        note = Note(items.get(0))

    }

    @Test
    fun test() {
        assertNotNull(note)
        assertEquals(note?.parent, "KEGNWTKP")
        assertEquals(note?.key, "GANIKZC7")
        assertEquals(note?.version, 7)
        assertEquals(
            note?.note,
            "<p>Wow great Article man</p>\n<p> </p>\n<p>here are some notes dude</p>\n<p> </p>"
        )
    }

    @Test
    fun testMissingNote() {
        val items2 = ItemJSONConverter().deserialize(json2)
        try {
            val note2 = Note(items2.get(0))
            assertNull(note2) //should not be here.
        } catch (e: ExceptionInInitializerError) {
            assertTrue(e.message == "No note")
        }
    }

    @Test
    fun testJson() {
        val note = Note("note text", "F#F#F#F")
        val jsonArray = note.asJsonArray()
        assert(jsonArray.toString() == "[{\"itemType\":\"note\",\"note\":\"note text\",\"parentItem\":\"F#F#F#F\",\"tags\":[],\"collections\":[],\"relations\":[]}]")
    }

    val json = """[{
        "key": "GANIKZC7",
        "version": 7,
        "library": {
            "type": "user",
            "id": 5884121,
            "name": "testacc1",
            "links": {
                "alternate": {
                    "href": "https://www.zotero.org/testacc1",
                    "type": "text/html"
                }
            }
        },
        "links": {
            "self": {
                "href": "https://api.zotero.org/users/5884121/items/GANIKZC7",
                "type": "application/json"
            },
            "alternate": {
                "href": "https://www.zotero.org/testacc1/items/GANIKZC7",
                "type": "text/html"
            },
            "up": {
                "href": "https://api.zotero.org/users/5884121/items/KEGNWTKP",
                "type": "application/json"
            }
        },
        "meta": {},
        "data": {
            "key": "GANIKZC7",
            "version": 7,
            "parentItem": "KEGNWTKP",
            "itemType": "note",
            "note": "<p>Wow great Article man</p>\n<p> </p>\n<p>here are some notes dude</p>\n<p> </p>",
            "tags": [],
            "relations": {},
            "dateAdded": "2019-08-12T04:15:09Z",
            "dateModified": "2019-08-12T04:15:19Z"
        }
    }]"""

    val json2 = """[{
        "key": "GANIKZC7",
        "version": 7,
        "meta": {},
        "data": {
            "key": "GANIKZC7",
            "version": 7,
            "parentItem": "KEGNWTKP",
            "itemType": "note",
            "tags": [],
            "relations": {},
            "dateAdded": "2019-08-12T04:15:09Z",
            "dateModified": "2019-08-12T04:15:19Z"
        }
    }]"""
}