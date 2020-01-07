package com.mickstarify.zooforzotero.ZoteroAPI.Model

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import java.lang.Exception

class Attachment {
    var itemKey: String
    var parentItem: String
    var itemType: String = "attachment"
    var linkMode: String = "imported_file"
    var filename: String
    var contentType: String = "application/pdf"
    var title: String = ""
    var accessDate: String = ""
    var note: String = ""
    var url: String = ""

    var md5: String = "null"

    constructor(item: Item) {
        itemKey = item.itemKey
        parentItem = item.data["parentItem"]
            ?: throw Exception("Error no parent key provided for attachment")
        itemType = item.itemType
        linkMode = "imported_file"
        title = item.getTitle()
        accessDate = item.data["accessDate"] ?: ""
        url = item.data["url"] ?: ""
        filename = item.data["filename"] ?: ""
    }

    constructor(title: String, filename: String, parent: String) {
        this.title = title
        this.filename = filename
        this.parentItem = parent
        this.itemKey = "" //hopefully this doesnt cause problems.
    }


    /* [ {
    "itemType": "attachment",
    "parentItem": "ABCD2345",
    "linkMode": "imported_url",
    "title": "My Document",
    "accessDate": "2012-03-14T17:45:54Z",
    "url": "http://example.com/doc.pdf",
    "note": "",
    "tags": [],
    "relations": {},
    "contentType": "application/pdf",
    "charset": "",
    "filename": "doc.pdf",
    "md5": null,
    "mtime": null
  } ]*/
    fun asNewJsonTemplate(): JsonArray {
        val jsonArray = JsonArray()
        val jsonObject = JsonObject()

        jsonObject.addProperty("itemType", itemType)
        jsonObject.addProperty("parentItem", parentItem)
        jsonObject.addProperty("linkMode", "imported_url")
        jsonObject.addProperty("title", title)
        jsonObject.addProperty("accessDate", accessDate)
        jsonObject.addProperty("url", url)
        jsonObject.addProperty("note", note)
        jsonObject.add("tags", JsonArray())
        jsonObject.add("relations", JsonObject())
        jsonObject.addProperty("contentType", contentType)
        jsonObject.addProperty("charset", "")
        jsonObject.addProperty("filename", filename)
        jsonObject.add("md5", JsonNull.INSTANCE)
        jsonObject.add("mtime", JsonNull.INSTANCE)

        jsonArray.add(jsonObject)
        return jsonArray
    }

    companion object {
        @JvmStatic
        fun getNewAttachmentTemplate(title: String, filename: String, parent: String): JsonArray {
            val attachment = Attachment(title, filename, parent)
            return attachment.asNewJsonTemplate()
        }
    }
}