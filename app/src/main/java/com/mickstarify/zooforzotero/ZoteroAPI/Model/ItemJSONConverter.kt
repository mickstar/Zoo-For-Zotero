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
        val lmap: List<Map<String, Any>> = LinkedList()
        val listOfItemsObjects = gson.fromJson(jsonString, lmap.javaClass)

        val items: MutableList<Item> = LinkedList()

        for (itemMap: Map<String, Any> in listOfItemsObjects) {
            try {
                val itemKey = (itemMap["key"] ?: "unknown") as String
                val version = (((itemMap["version"]) as Double).toInt())


                val tags: MutableList<String> = LinkedList()
                val creators = LinkedList<Creator>()
                val data: MutableMap<String, String> = HashMap()
                val itemDataMap = ((itemMap["data"] ?: HashMap<String, Any>()) as Map<String, Any>)
                var collections: List<String> = LinkedList()
                var mtime: Double = 0.0
                for ((key: String, value: Any) in itemDataMap) {
                    when (key) {
                        "tags" -> {
                            for (tagItem: Map<String, String> in (value as List<Map<String, String>>)) {
                                val tag = tagItem.get("tags")
                                if (tag != null) {
                                    tags.add(tag)
                                }
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
                            if (value is List<*>) {
                                collections = value as List<String>
                            }
                        }
                        "version" -> {
                            //ignore
                        }
                        "relations" -> {
                            //ignore, don't care lol.
                        }
                        "mtime" -> {
                            if (value is Double) {
                                mtime = value
                            }
                        }
                        else -> {
                            if (value is String) {
                                data[key] = value
                            } else if (value is Int) {
                                data[key] = "$value"
                            } else if (value is Boolean) {
                                data[key] = "$value"
                            } else {
                                Log.e(
                                    "zotero",
                                    "This parameter '$key' is not a string and not logged"
                                )
                            }
                        }
                    }
                }

                items.add(Item(itemKey, version, data, tags, creators, collections, mtime))
            } catch (e: Exception) {
                Log.e("Zotero", "Error occurred when adding item. Skipping this item.")
            }
        }
        return items
    }
}