package com.mickstarify.zooforzotero.ZoteroAPI.Model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.*

class Note() : Parcelable {
    lateinit var parent: String
    lateinit var key: String
    lateinit var note: String
    lateinit var tags: List<String>

    constructor(parcel: Parcel) : this() {
        parent = parcel.readString() ?: throw ExceptionInInitializerError("No Parent Key")
        key = parcel.readString() ?: throw ExceptionInInitializerError("No Key")
        note = parcel.readString() ?: throw ExceptionInInitializerError("No note")
        tags = parcel.createStringArrayList() ?: LinkedList()
    }

    constructor(item: Item) : this() {
        parent = item.data["parentItem"] ?: throw ExceptionInInitializerError("No Parent Key")
        key = item.data["key"] ?: throw ExceptionInInitializerError("No Key")
        note = item.data["note"] ?: throw ExceptionInInitializerError("No note")
        tags = item.tags
    }

    constructor(note: String, parent: String) : this() {
        this.parent = parent
        this.note = note
        this.tags = LinkedList()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(parent)
        parcel.writeString(key)
        parcel.writeString(note)
        parcel.writeStringList(tags)
    }

    fun writeToZoteroJson(): JsonArray {
        val jsonArray = JsonArray()
        val noteObject = JsonObject()

        noteObject.addProperty("itemType", "note")
        noteObject.addProperty("note", note)
        noteObject.addProperty("parentItem", parent)
        noteObject.add("tags", JsonArray())
        noteObject.add("collections", JsonArray())
        noteObject.add("relations", JsonArray())
        jsonArray.add(noteObject)
        return jsonArray


//        return """[{
//        "itemType": "note",
//        "note": "$note",
//        "parentItem": "$parent",
//        "tags": [],
//        "collections": [],
//        "relations": {}
//        }]""".trimMargin()
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Note> {
        override fun createFromParcel(parcel: Parcel): Note {
            return Note(parcel)
        }

        override fun newArray(size: Int): Array<Note?> {
            return arrayOfNulls(size)
        }
    }
}