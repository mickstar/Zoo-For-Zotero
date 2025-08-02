package com.mickstarify.zooforzotero.ZoteroAPI.Model

import android.util.Log
import com.google.gson.Gson
import java.util.LinkedList

/* parses the output of the zotero item json and creates a itemPOJO object. */
class ItemJSONConverter {
    fun deserialize(jsonString: String): List<ItemPOJO> {
        if (jsonString == "[]") {
            return LinkedList()
        }
        val gson = Gson()
        val lmap: List<Map<String, Any>> = LinkedList()
        val listOfItemsObjects = gson.fromJson(jsonString, lmap.javaClass)

        val items: MutableList<ItemPOJO> = LinkedList()

        for (itemMap: Map<String, Any> in listOfItemsObjects) {
            try {
                val itemKey = (itemMap["key"] ?: "unknown") as String
                val version = (((itemMap["version"]) as Double).toInt())


                val tags: MutableList<String> = LinkedList()
                val creators = LinkedList<CreatorPOJO>()
                val data: MutableMap<String, String> = HashMap()
                val itemDataMap = ((itemMap["data"] ?: HashMap<String, Any>()) as Map<String, Any>)
                var collections: List<String> = LinkedList()
                var mtime: Double = 0.0
                var deleted = 0
                for ((key: String, value: Any) in itemDataMap) {
                    when (key) {
                        "tags" -> {
                            for (tagItem: Map<String, String> in (value as List<Map<String, String>>)) {
                                val tag = tagItem["tag"]
                                if (tag != null) {
                                    tags.add(tag)
                                }
                            }
                        }
                        "creators" -> {
                            for (creator: Map<String, String> in value as List<Map<String, String>>) {
                                creators.add(
                                    CreatorPOJO(
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
                        "deleted" -> {
                            deleted = (value as Double).toInt()
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

                items.add(ItemPOJO(itemKey, version, data, tags, creators, collections, mtime, deleted))
            } catch (e: Exception) {
                Log.e("Zotero", "Error occurred when adding item. Skipping this item.")
                Log.e("zotero", e.toString())
            }
        }
        return items
    }
}