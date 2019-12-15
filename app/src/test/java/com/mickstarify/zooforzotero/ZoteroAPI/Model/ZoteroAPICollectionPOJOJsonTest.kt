package com.mickstarify.zooforzotero.ZoteroAPI.Model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZoteroAPICollectionPOJOJsonTest {
    var gson = Gson()

    @Before
    fun setUp() {
    }

    @Test
    fun deserialize() {
        val listType = object : TypeToken<List<CollectionPOJO>>() {}.type
        val collection: List<CollectionPOJO> = gson.fromJson(collectionJSONSample, listType)
        assertTrue(collection.isNotEmpty())
        assertTrue(collection.filter {it.key == "RHDBUR9L"}.get(0).getName() == "Haskell")
        assertTrue(collection.filter {it.key == "RHDBUR9L"}.get(0).hasParent())
    }

    val collectionJSONSample = """[
    {
        "key": "RHDBUR9L",
        "version": 8,
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
                "href": "https://api.zotero.org/users/5884121/collections/RHDBUR9L",
                "type": "application/json"
            },
            "alternate": {
                "href": "https://www.zotero.org/testacc1/collections/RHDBUR9L",
                "type": "text/html"
            },
            "up": {
                "href": "https://api.zotero.org/users/5884121/collections/96CDCVXN",
                "type": "application/atom+xml"
            }
        },
        "meta": {
            "numCollections": 0,
            "numItems": 1
        },
        "data": {
            "key": "RHDBUR9L",
            "version": 8,
            "name": "Haskell",
            "parentCollection": "96CDCVXN",
            "relations": {}
        }
    },
    {
        "key": "JHUCHVIT",
        "version": 6,
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
                "href": "https://api.zotero.org/users/5884121/collections/JHUCHVIT",
                "type": "application/json"
            },
            "alternate": {
                "href": "https://www.zotero.org/testacc1/collections/JHUCHVIT",
                "type": "text/html"
            }
        },
        "meta": {
            "numCollections": 0,
            "numItems": 1
        },
        "data": {
            "key": "JHUCHVIT",
            "version": 6,
            "name": "Philosophy",
            "parentCollection": false,
            "relations": {}
        }
    },
    {
        "key": "96CDCVXN",
        "version": 2,
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
                "href": "https://api.zotero.org/users/5884121/collections/96CDCVXN",
                "type": "application/json"
            },
            "alternate": {
                "href": "https://www.zotero.org/testacc1/collections/96CDCVXN",
                "type": "text/html"
            }
        },
        "meta": {
            "numCollections": 1,
            "numItems": 2
        },
        "data": {
            "key": "96CDCVXN",
            "version": 2,
            "name": "Programming",
            "parentCollection": false,
            "relations": {}
        }
    }
]"""
}
