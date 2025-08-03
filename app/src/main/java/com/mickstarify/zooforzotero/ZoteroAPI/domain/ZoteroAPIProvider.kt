package com.mickstarify.zooforzotero.ZoteroAPI.domain

import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.SyncSetup.domain.AuthenticationStorage
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroAPI
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider for ZoteroAPI instances with lazy singleton behavior.
 * 
 * Creates and caches a ZoteroAPI instance only when credentials are available
 * and the API is first requested. Subsequent calls return the cached instance.
 */
interface ZoteroAPIProvider {
    
    /**
     * Gets the ZoteroAPI instance, creating it lazily if credentials are available.
     * 
     * @return ZoteroAPI instance if credentials exist, null otherwise
     */
    fun getAPI(): ZoteroAPI?
    
    /**
     * Clears the cached API instance. Call this when credentials change
     * (e.g., user logs out or switches accounts).
     */
    fun clearCache()
}

@Singleton
class ZoteroAPIProviderImpl @Inject constructor(
    private val authenticationStorage: AuthenticationStorage,
    private val attachmentStorageManager: AttachmentStorageManager,
    private val preferenceManager: PreferenceManager
) : ZoteroAPIProvider {
    
    private var cachedAPI: ZoteroAPI? = null
    
    override fun getAPI(): ZoteroAPI? {
        // Return cached instance if available
        cachedAPI?.let { return it }
        
        // Create new instance only if credentials are available
        return if (authenticationStorage.hasCredentials()) {
            val newAPI = ZoteroAPI(
                authenticationStorage.getUserKey(),
                authenticationStorage.getUserID(),
                authenticationStorage.getUsername(),
                attachmentStorageManager,
                preferenceManager
            )
            cachedAPI = newAPI
            newAPI
        } else {
            null
        }
    }
    
    override fun clearCache() {
        cachedAPI = null
    }
}