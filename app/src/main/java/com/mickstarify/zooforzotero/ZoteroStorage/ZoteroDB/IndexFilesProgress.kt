package com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB

data class IndexFilesProgress (
    val currentIndex: Int,
    val totalNumber: Int,
    val currentFilename: String
)