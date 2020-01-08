package com.mickstarify.zooforzotero.di.component

import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase

interface DatabaseComponent {
    val zoteroDatabase: ZoteroDatabase
}