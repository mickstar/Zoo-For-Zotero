package com.mickstarify.zooforzotero.di

import android.content.Context
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.SyncSetup.domain.ApiKeyRepository
import com.mickstarify.zooforzotero.SyncSetup.domain.ApiKeyRepositoryImpl
import com.mickstarify.zooforzotero.SyncSetup.domain.AuthenticationStorage
import com.mickstarify.zooforzotero.SyncSetup.domain.AuthenticationStorageImpl
import com.mickstarify.zooforzotero.AttachmentManager.domain.AttachmentStorageRepository
import com.mickstarify.zooforzotero.AttachmentManager.domain.AttachmentStorageRepositoryImpl
import com.mickstarify.zooforzotero.ZoteroAPI.domain.ZoteroAPIProvider
import com.mickstarify.zooforzotero.ZoteroAPI.domain.ZoteroAPIProviderImpl
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase
import com.mickstarify.zooforzotero.ZoteroStorage.domain.ZoteroRepository
import com.mickstarify.zooforzotero.ZoteroStorage.domain.ZoteroRepositoryImpl
import com.mickstarify.zooforzotero.common.DispatchersProvider
import com.mickstarify.zooforzotero.common.DispatchersProviderImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SingletonModule {

    fun providePreferenceManager(@ApplicationContext context: Context): PreferenceManager {
        return PreferenceManager(context)
    }

    @Singleton
    @Provides
    fun providesAttachmentStorageManager(
        context: Context,
        preferenceManager: PreferenceManager
    ): AttachmentStorageManager {
        return AttachmentStorageManager(context, preferenceManager)
    }

    @Singleton
    @Provides
    fun getDatabase(context: Context): ZoteroDatabase {
        return ZoteroDatabase(context)
    }

    @Provides
    fun provideAppContext(@ApplicationContext appContext: Context): Context {
        return appContext
    }

    @Singleton
    @Provides
    fun provideAuthenticationStorage(@ApplicationContext context: Context): AuthenticationStorage {
        return AuthenticationStorageImpl(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindApiKeyRepository(impl: ApiKeyRepositoryImpl): ApiKeyRepository
    
    @Binds
    abstract fun bindZoteroRepository(impl: ZoteroRepositoryImpl): ZoteroRepository
    
    @Binds
    abstract fun bindAttachmentStorageRepository(impl: AttachmentStorageRepositoryImpl): AttachmentStorageRepository
    
    @Binds
    abstract fun bindZoteroAPIProvider(impl: ZoteroAPIProviderImpl): ZoteroAPIProvider
    
    @Binds
    abstract fun bindDispatchersProvider(impl: DispatchersProviderImpl): DispatchersProvider
}