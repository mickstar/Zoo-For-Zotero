package com.mickstarify.zooforzotero.ZoteroAPI.Model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
open class Item(
    @SerializedName("ItemKey")
    var ItemKey: String,
    @SerializedName("version")
    var version: Int,
    @SerializedName("data")
    var data: Map<String, String>,
    @SerializedName("tags")
    var tags: List<String>,
    val creators: List<Creator>,
    val collections: List<String>,
    var mtime: Double = 0.0
) : Parcelable {
    companion object {
        val ATTACHMENT_TYPE = "attachment"
    }

    @IgnoredOnParcel
    var localMd5 : String = ""

    fun getValue(key: String): Any? {
        when (key) {
            "key" -> return this.ItemKey
            "version" -> return this.version
            "tags" -> return this.tags
            "mtime" -> return this.mtime.toString()
            else -> {
                if (key in data) {
                    when (key) {
                        "creators" -> return creators
                        else -> {
                            if (data[key] is String) {
                                return data[key] ?: ""
                            }
                            return null
                        }
                    }
                } else {
                    return null
                }
            }
        }
    }

    fun getTitle(): String {
        val title = when (getItemType()) {
            "case" -> this.getValue("caseName")
            "statute" -> this.getValue("nameOfAct")
            else -> this.getValue("title")
        }

        return (title ?: "unknown") as String
    }

    fun getItemType(): String {
        return if ("itemType" in data) {
            (data["itemType"] as String)
        } else {
            "error"
        }
    }

    fun getAuthor(): String {
        return when (creators.size) {
            0 -> ""
            1 -> creators[0].lastName
            else -> "${creators[0].lastName} et al."
        }
    }

    /* Matches the query text against the metadata stored in item,
    * checks to see if we can find the text anywhere. Useful for search. */
    fun query(queryText: String): Boolean {
        val queryUpper = queryText.toUpperCase(Locale.ROOT)

        return this.ItemKey.toUpperCase(Locale.ROOT).contains(queryUpper) ||
                this.tags.joinToString("_").toUpperCase(Locale.ROOT).contains(queryUpper) ||
                this.data.values.joinToString("_").toUpperCase(Locale.ROOT).contains(
                    queryUpper
                ) || this.creators.map {
            it.makeString().toUpperCase(Locale.ROOT)
        }.joinToString("_").contains(queryUpper)
    }

    fun getSortableDateString(): String {
        val date = data["date"] ?: ""
        if (date == "") {
            return "ZZZZ"
        }
        return date
    }

    fun getSortableDateAddedString(): String {
        return data["dateAdded"] ?: "XXXX-XX-XX"
    }
}

/* Maps itemType name to Localized name */
val itemTypes: HashMap<String, String> = hashMapOf(
    "artwork" to "Artwork",
    "audioRecording" to "Audio Recording",
    "bill" to "Bill",
    "blogPost" to "Blog Post",
    "book" to "Book",
    "bookSection" to "Book Section",
    "case" to "Case",
    "computerProgram" to "Computer Program",
    "conferencePaper" to "Conference Paper",
    "dictionaryEntry" to "Dictionary Entry",
    "document" to "Document",
    "email" to "E-mail",
    "encyclopediaArticle" to "Encyclopedia Article",
    "film" to "Film",
    "forumPost" to "Forum Post",
    "hearing" to "Hearing",
    "instantMessage" to "Instant Message",
    "interview" to "Interview",
    "journalArticle" to "Journal Article",
    "letter" to "Letter",
    "magazineArticle" to "Magazine Article",
    "manuscript" to "Manuscript",
    "map" to "Map",
    "newspaperArticle" to "Newspaper Article",
    "note" to "Note",
    "patent" to "Patent",
    "podcast" to "Podcast",
    "presentation" to "Presentation",
    "radioBroadcast" to "Radio Broadcast",
    "report" to "Report",
    "statute" to "Statute",
    "tvBroadcast" to "TV Broadcast",
    "thesis" to "Thesis",
    "videoRecording" to "Video Recording",
    "webpage" to "Web Page",
    "unknown" to "Unknown"
)