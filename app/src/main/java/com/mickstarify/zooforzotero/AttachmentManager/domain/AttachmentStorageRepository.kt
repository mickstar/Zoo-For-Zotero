package com.mickstarify.zooforzotero.AttachmentManager.domain

import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import com.mickstarify.zooforzotero.common.DispatchersProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository for attachment file storage operations.
 * 
 * Provides a clean interface for checking attachment file existence, calculating sizes,
 * and other file system operations related to Zotero attachments.
 */
interface AttachmentStorageRepository {

    /**
     * Represents metadata about an attachment file on the local filesystem.
     */
    data class AttachmentFileMetadata(
        val exists: Boolean,
        val sizeBytes: Long
    )

    /**
     * Checks if an attachment file exists locally and returns its metadata.
     * 
     * @param item The attachment item to check
     * @param checkMd5 Whether to verify the MD5 hash of the file
     * @return [AttachmentFileMetadata] containing existence and size information
     */
    suspend fun getAttachmentMetadata(item: Item, checkMd5: Boolean = false): AttachmentFileMetadata
}

class AttachmentStorageRepositoryImpl @Inject constructor(
    private val attachmentStorageManager: AttachmentStorageManager,
    private val dispatchersProvider: DispatchersProvider
) : AttachmentStorageRepository {

    override suspend fun getAttachmentMetadata(
        item: Item, 
        checkMd5: Boolean
    ): AttachmentStorageRepository.AttachmentFileMetadata =
        withContext(dispatchersProvider.io) {
            val exists = attachmentStorageManager.checkIfAttachmentExists(item, checkMd5)
            val sizeBytes = if (exists) {
                attachmentStorageManager.getFileSize(item)
            } else {
                0L
            }
            
            AttachmentStorageRepository.AttachmentFileMetadata(
                exists = exists,
                sizeBytes = sizeBytes
            )
        }
}