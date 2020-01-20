package com.mickstarify.zooforzotero.ZoteroAPI.Model

data class DeletedEntriesPojo (
    val collections: List<String>,
    val items: List<String>,
    val searches: List<String>,
    val tags: List<String>,
    val settings: List<String>
){

}