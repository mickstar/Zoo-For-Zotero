package com.mickstarify.zooforzotero.ZoteroStorage.Database

import android.os.Bundle
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mickstarify.zooforzotero.ZoteroAPI.Model.ItemJSONConverter
import com.mickstarify.zooforzotero.ZoteroAPI.Model.ItemPOJO
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ItemTest {
    lateinit var zoteroDatabase : ZoteroDatabase

    lateinit var itemPOJO: ItemPOJO

    @Before
    fun setUp() {
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().targetContext

        zoteroDatabase = ZoteroDatabase(instrumentationContext)
        itemPOJO = ItemJSONConverter().deserialize("""[{
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
    }]""").first()
    }

//    @Test
//    fun testAddItem(){
//        zoteroDatabase.writeItem(-1, itemPOJO).doOnComplete {
//            Log.d("zotero", "finished writing item")
//            val items = zoteroDatabase.getItemsForGroup(-1).blockingGet()
//            assertTrue(items.isNotEmpty())
//        }.doOnError { throw (it) }.subscribe()
//    }

//    @Test
//    fun testAddItem(){
//        Log.d("zotero", "inserting item ${itemPOJO.ItemKey}")
//        zoteroDatabase.writeItem(-1, itemPOJO)
//        val items = zoteroDatabase.getItemsForGroup(-1).blockingGet()
//        assertTrue(items.isNotEmpty())
//    }

    @Test
    fun testAddItemPOJO(){
        Log.d("zotero", "inserting item POJO ${itemPOJO.ItemKey}")
        zoteroDatabase.writeItem(-1, itemPOJO).blockingGet()
        val items = zoteroDatabase.getItemsForGroup(-1).blockingGet()
        assertTrue(items.isNotEmpty())
        val item = items.filter { it.itemKey == "KEGNWTKP" }.first()
        assertTrue(item.creators.first().firstName == "Ludwig")
        assertTrue(item.collections.isNotEmpty() )
        assertTrue(item.collections.first() == "JHUCHVIT")
    }

    @Test
    fun parcelTest(){
        zoteroDatabase.writeItem(-1, itemPOJO).blockingGet()
        val items = zoteroDatabase.getItemsForGroup(-1).blockingGet()
        assertTrue(items.isNotEmpty())
        val item = items.filter { it.itemKey == "KEGNWTKP" }.first()

        val bundle = Bundle().apply { putParcelable("item", item) }
        val item2 = bundle.getParcelable<Item>("item") as Item
        assertTrue(item.itemType == item2.itemType)
        assertTrue(item.creators.first().firstName == item2.creators.first().firstName)
        assertTrue(item.itemInfo.itemKey == item2.itemKey)
    }


}