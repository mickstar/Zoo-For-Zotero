package com.mickstarify.zooforzotero.ZoteroAPI

import com.mickstarify.zooforzotero.ZoteroAPI.Model.ZoteroItemTemplates
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Creator
import org.json.JSONObject

enum class ItemType {
    artwork,
    audioRecording,
    bill,
    blogPost,
    book,
    bookSection,
    case,
    computerProgram,
    conferencePaper,
    dictionaryEntry,
    document,
    email,
    encyclopediaArticle,
    film,
    forumPost,
    hearing,
    instantMessage,
    interview,
    journalArticle,
    letter,
    magazineArticle,
    manuscript,
    map,
    newspaperArticle,
    note,
    patent,
    podcast,
    presentation,
    radioBroadcast,
    report,
    statute,
    tvBroadcast,
    thesis,
    videoRecording,
    webpage
}

class ItemTemplate (val itemType: ItemType) {
    lateinit var jsonObject: JSONObject

    init {
        jsonObject = when (itemType) {
            ItemType.book -> JSONObject( ZoteroItemTemplates.ITEM_BOOK_JSON )
            else -> throw (Exception("Error unknown item Type"))
        }
    }

    fun addCreator() {

    }

    fun addCreator(creator: Creator){
        val creatorObject = jsonObject.getJSONObject("creator")
        creatorObject.put("creatorType", "")
        creatorObject.put("", "")
        creatorObject.put("", "")
    }

    fun setValue(key: String, value: String){

    }

    fun addCollection(collectionKey: String){

    }

    fun addTag(tag: String){

    }
}