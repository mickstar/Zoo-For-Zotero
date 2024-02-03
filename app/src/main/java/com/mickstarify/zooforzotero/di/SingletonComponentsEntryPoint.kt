package com.mickstarify.zooforzotero.di

import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SingletonComponentsEntryPoint {
    fun getZoteroDatabase(): ZoteroDatabase
    fun getAttachmentStorageManager(): AttachmentStorageManager
    fun getPreferences(): PreferenceManager
}