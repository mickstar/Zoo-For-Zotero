package com.mickstarify.zooforzotero.ZoteroAPI.Model

import android.util.Log
import com.google.gson.Gson
import java.util.*
import kotlin.collections.HashMap

/* I had to write a custom json deserializer because the Zotero api has an inconsistent json output.
* which is to say that return entries have varied parameters depending on the itemType or what info is available.*/
class ItemJSONConverter {
    fun deserialize(jsonString: String): List<Item> {
        val gson = Gson()
        var lmap: List<Map<String, Any>> = LinkedList()
        val listOfItemsObjects = gson.fromJson(jsonString, lmap.javaClass)

        val items: MutableList<Item> = LinkedList()

        for (itemMap: Map<String, Any> in listOfItemsObjects) {
            val itemKey = (itemMap["key"] ?: "unknown") as String
            val version = (((itemMap["version"]) as Double).toInt())


            var tags: MutableList<String> = LinkedList()
            val creators = LinkedList<Creator>()
            var data: MutableMap<String, String> = HashMap()
            val itemDataMap = ((itemMap["data"] ?: HashMap<String, Any>()) as Map<String, Any>)
            var collections : List<String> = LinkedList()
            for ((key: String, value: Any) in itemDataMap) {
                when (key) {
                    "tags" -> {
                        for (tagItem: Map<String, String> in (value as List<Map<String, String>>)) {
                            tags.add(tagItem.getOrDefault("tags", ""))
                        }
                    }
                    "creators" -> {
                        for (creator: Map<String, String> in value as List<Map<String, String>>) {
                            creators.add(
                                Creator(
                                    creator["creatorType"] ?: "unknown",
                                    creator["firstName"] ?: "",
                                    creator["lastName"] ?: ""
                                )
                            )
                        }
                    }
                    "collections" -> {
                        collections = value as List<String>
                    }
                    "version" -> {
                        //ignore
                    }
                    "relations" -> {
                        //ignore, don't care lol.
                    }
                    else -> {
                        if (value is String) {
                            data[key] = value
                        } else if (value is Int) {
                            data[key] = "$value"
                        } else {
                            Log.d("zotero", "This parameter '$key' is not a string and not logged")
                        }
                    }
                }
            }

            items.add(Item(itemKey, version, data, tags, creators, collections))
        }

        return items
    }
}