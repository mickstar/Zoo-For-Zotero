package com.mickstarify.zooforzotero.AttachmentManager.domain

import android.util.Log
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroDB
import com.mickstarify.zooforzotero.common.DispatchersProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

private const val TAG = "CalculateAttachmentMetadataUseCase"

/**
 * Use case for calculating attachment metadata by scanning the local filesystem.
 * 
 * Processes downloadable attachment items and emits progressive updates about
 * their local storage status including file existence and sizes.
 */
class CalculateAttachmentMetadataUseCase @Inject constructor(
    private val attachmentStorageRepository: AttachmentStorageRepository,
    private val dispatchersProvider: DispatchersProvider
) {

    /**
     * Represents the current progress of metadata calculation.
     */
    data class MetadataProgress(
        val localAttachmentCount: Int,
        val totalSizeBytes: Long,
        val totalAttachmentCount: Int,
        val isComplete: Boolean = false
    )

    /**
     * Calculates attachment metadata for all downloadable items in the library.
     * 
     * @param zoteroDB The loaded Zotero database containing items
     * @return [Flow] of [MetadataProgress] updates as files are processed
     */
    suspend fun execute(zoteroDB: ZoteroDB): Flow<MetadataProgress> = flow {
        val attachmentItems = zoteroDB.items
            ?.filter { it.itemType == "attachment" && it.isDownloadable() }
            ?: emptyList()

        Log.d(TAG, "Starting metadata calculation for ${attachmentItems.size} attachments")

        var localAttachmentCount = 0
        var totalSizeBytes = 0L

        for (item in attachmentItems) {
            if (!item.isDownloadable() || item.data["linkMode"] == "linked_file") {
                continue
            }

            Log.d(TAG, "Checking if ${item.data["filename"]} exists")
            
            val metadata = attachmentStorageRepository.getAttachmentMetadata(item, checkMd5 = false)
            
            if (metadata.exists) {
                localAttachmentCount++
                totalSizeBytes += metadata.sizeBytes
            }

            // Emit progress update
            emit(
                MetadataProgress(
                    localAttachmentCount = localAttachmentCount,
                    totalSizeBytes = totalSizeBytes,
                    totalAttachmentCount = attachmentItems.size,
                    isComplete = false
                )
            )
        }

        // Emit final completion
        emit(
            MetadataProgress(
                localAttachmentCount = localAttachmentCount,
                totalSizeBytes = totalSizeBytes,
                totalAttachmentCount = attachmentItems.size,
                isComplete = true
            )
        )

        Log.d(TAG, "Metadata calculation complete: $localAttachmentCount/$attachmentItems.size files, ${totalSizeBytes} bytes")
    }.flowOn(dispatchersProvider.io)
}