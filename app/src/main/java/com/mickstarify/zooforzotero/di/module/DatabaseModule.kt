package com.mickstarify.zooforzotero.di.module

import android.content.Context
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun getDatabase(context: Context): ZoteroDatabase {
        return ZoteroDatabase(context)
    }
}