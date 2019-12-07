package com.mickstarify.zooforzotero.ZoteroAPI.Database

import dagger.Component
import javax.inject.Singleton

@Component
@Singleton
interface DatabaseComponent {
    val zoteroDatabase: ZoteroDatabase
}