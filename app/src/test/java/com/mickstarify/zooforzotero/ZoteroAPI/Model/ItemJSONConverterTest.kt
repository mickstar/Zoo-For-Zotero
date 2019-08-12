package com.mickstarify.zooforzotero.ZoteroAPI.Model

import org.junit.Before
import org.junit.Test

class ItemJSONConverterTest {
    lateinit var jsonConverter : ItemJSONConverter

    @Before
    fun setUp() {
        jsonConverter = ItemJSONConverter()

    }

    @Test
    fun deserialize() {
        jsonConverter.deserialize(sampleItemJson)
    }

    val sampleItemJson = """
        [
    {
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
    },
    {
        "key": "KEGNWTKP",
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
                "href": "https://api.zotero.org/users/5884121/items/KEGNWTKP",
                "type": "application/json"
            },
            "alternate": {
                "href": "https://www.zotero.org/testacc1/items/KEGNWTKP",
                "type": "text/html"
            }
        },
        "meta": {
            "creatorSummary": "Wittgenstein",
            "parsedDate": "2007",
            "numChildren": 1
        },
        "data": {
            "key": "KEGNWTKP",
            "version": 7,
            "itemType": "book",
            "title": "Wittgenstein: Lectures and Conversations on Aesthetics, Psychology and Religious Belief",
            "creators": [
                {
                    "creatorType": "author",
                    "firstName": "Ludwig",
                    "lastName": "Wittgenstein"
                }
            ],
            "abstractNote": "",
            "series": "",
            "seriesNumber": "",
            "volume": "",
            "numberOfVolumes": "",
            "edition": "",
            "place": "",
            "publisher": "Univ of California Press",
            "date": "2007",
            "numPages": "",
            "language": "",
            "ISBN": "0-520-25181-4",
            "shortTitle": "",
            "url": "",
            "accessDate": "",
            "archive": "",
            "archiveLocation": "",
            "libraryCatalog": "",
            "callNumber": "",
            "rights": "",
            "extra": "",
            "tags": [],
            "collections": [
                "JHUCHVIT"
            ],
            "relations": {},
            "dateAdded": "2019-08-12T04:14:59Z",
            "dateModified": "2019-08-12T04:14:59Z"
        }
    },
    {
        "key": "LMPBTEAF",
        "version": 5,
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
                "href": "https://api.zotero.org/users/5884121/items/LMPBTEAF",
                "type": "application/json"
            },
            "alternate": {
                "href": "https://www.zotero.org/testacc1/items/LMPBTEAF",
                "type": "text/html"
            }
        },
        "meta": {
            "creatorSummary": "Viola and Jones",
            "parsedDate": "2004",
            "numChildren": 0
        },
        "data": {
            "key": "LMPBTEAF",
            "version": 5,
            "itemType": "journalArticle",
            "title": "Robust real-time face detection",
            "creators": [
                {
                    "creatorType": "author",
                    "firstName": "Paul",
                    "lastName": "Viola"
                },
                {
                    "creatorType": "author",
                    "firstName": "Michael J.",
                    "lastName": "Jones"
                }
            ],
            "abstractNote": "",
            "publicationTitle": "International journal of computer vision",
            "volume": "57",
            "issue": "2",
            "pages": "137-154",
            "date": "2004",
            "series": "",
            "seriesTitle": "",
            "seriesText": "",
            "journalAbbreviation": "",
            "language": "",
            "DOI": "",
            "ISSN": "",
            "shortTitle": "",
            "url": "",
            "accessDate": "",
            "archive": "",
            "archiveLocation": "",
            "libraryCatalog": "",
            "callNumber": "",
            "rights": "",
            "extra": "",
            "tags": [],
            "collections": [
                "96CDCVXN"
            ],
            "relations": {},
            "dateAdded": "2019-08-12T04:14:20Z",
            "dateModified": "2019-08-12T04:14:20Z"
        }
    },
    {
        "key": "V4B3QVLT",
        "version": 3,
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
                "href": "https://api.zotero.org/users/5884121/items/V4B3QVLT",
                "type": "application/json"
            },
            "alternate": {
                "href": "https://www.zotero.org/testacc1/items/V4B3QVLT",
                "type": "text/html"
            }
        },
        "meta": {
            "creatorSummary": "Bird and Wadler",
            "parsedDate": "1988",
            "numChildren": 0
        },
        "data": {
            "key": "V4B3QVLT",
            "version": 3,
            "itemType": "book",
            "title": "Functional programming",
            "creators": [
                {
                    "creatorType": "author",
                    "firstName": "R. S.",
                    "lastName": "Bird"
                },
                {
                    "creatorType": "author",
                    "firstName": "P. L.",
                    "lastName": "Wadler"
                }
            ],
            "abstractNote": "",
            "series": "",
            "seriesNumber": "",
            "volume": "",
            "numberOfVolumes": "",
            "edition": "",
            "place": "",
            "publisher": "Prentice Hall",
            "date": "1988",
            "numPages": "",
            "language": "",
            "ISBN": "",
            "shortTitle": "",
            "url": "",
            "accessDate": "",
            "archive": "",
            "archiveLocation": "",
            "libraryCatalog": "",
            "callNumber": "",
            "rights": "",
            "extra": "",
            "tags": [],
            "collections": [
                "96CDCVXN"
            ],
            "relations": {},
            "dateAdded": "2019-08-12T04:13:59Z",
            "dateModified": "2019-08-12T04:13:59Z"
        }
    }
]
        """
}
