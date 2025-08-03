package com.mickstarify.zooforzotero.ZoteroStorage.domain

import android.content.Context
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.ZoteroAPI.DownloadProgress
import com.mickstarify.zooforzotero.ZoteroAPI.domain.ZoteroAPIProvider
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.AttachmentInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import com.mickstarify.zooforzotero.ZoteroStorage.Database.ZoteroDatabase
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroDB
import com.mickstarify.zooforzotero.common.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository for managing Zotero library data operations.
 * 
 * Provides a clean interface for loading and accessing Zotero library items,
 * abstracting away the underlying database and storage implementation details.
 */
interface ZoteroRepository {

    /**
     * Loads library items from the local database.
     * 
     * @param groupId The group ID to load items for. Defaults to [GroupInfo.NO_GROUP_ID] for personal library.
     * @return [Result] containing the loaded [ZoteroDB] instance on success, or an error on failure.
     */
    suspend fun loadLibrary(groupId: Int = GroupInfo.NO_GROUP_ID): Result<ZoteroDB>

    /**
     * Downloads an attachment from the Zotero API and saves it locally.
     * 
     * @param item The attachment item to download
     * @param groupId The group ID the attachment belongs to
     * @return [Flow] of [DownloadProgress] updates during the download process
     */
    suspend fun downloadAttachment(item: Item, groupId: Int): Flow<DownloadProgress>
}

class ZoteroRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchersProvider: DispatchersProvider,
    private val zoteroDatabase: ZoteroDatabase,
    private val attachmentStorageManager: AttachmentStorageManager,
    private val preferenceManager: PreferenceManager,
    private val zoteroAPIProvider: ZoteroAPIProvider
) : ZoteroRepository {

    override suspend fun loadLibrary(groupId: Int): Result<ZoteroDB> =
        withContext(dispatchersProvider.io) {
            runCatching {
                val zoteroDB = ZoteroDB(context = context, groupID = groupId)
                zoteroDB.loadItemsFromDatabase().blockingAwait()
                zoteroDB
            }
        }

    override suspend fun downloadAttachment(item: Item, groupId: Int): Flow<DownloadProgress> = 
        callbackFlow {
            // Check if attachment already exists
            if (attachmentStorageManager.checkIfAttachmentExists(item, checkMd5 = false)) {
                // File already exists, emit completion immediately
                trySend(DownloadProgress(progress = 1L, total = 1L))
                close()
                return@callbackFlow
            }

            // Get ZoteroAPI instance from provider
            val zoteroAPI = zoteroAPIProvider.getAPI() ?: run {
                close(NoCredentialsException())
                return@callbackFlow
            }

            // Convert RxJava Observable to Flow
            var hasSetMetadata = false
            val downloadDisposable = zoteroAPI.downloadItemRx(item, groupId, context)
                .subscribe(
                    { downloadProgress ->
                        // Save attachment metadata to database when available
                        if (!hasSetMetadata && downloadProgress.metadataHash.isNotEmpty()) {
                            val attachmentInfo = AttachmentInfo(
                                item.itemKey,
                                groupId,
                                downloadProgress.metadataHash,
                                downloadProgress.mtime,
                                if (preferenceManager.isWebDAVEnabled()) {
                                    AttachmentInfo.WEBDAV
                                } else {
                                    AttachmentInfo.ZOTEROAPI
                                }
                            )
                            
                            // Save to database (blocking call for now)
                            zoteroDatabase.writeAttachmentInfo(attachmentInfo).blockingAwait()
                            hasSetMetadata = true
                        }

                        // Emit progress update
                        trySend(downloadProgress)
                    },
                    { error ->
                        close(error)
                    },
                    {
                        // On complete
                        close()
                    }
                )

            awaitClose {
                // Cleanup if needed
            }
        }.flowOn(dispatchersProvider.io)
}