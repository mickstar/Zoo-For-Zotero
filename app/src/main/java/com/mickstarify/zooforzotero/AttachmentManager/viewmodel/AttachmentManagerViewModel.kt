package com.mickstarify.zooforzotero.AttachmentManager.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mickstarify.zooforzotero.AttachmentManager.domain.CalculateAttachmentMetadataUseCase
import com.mickstarify.zooforzotero.ZoteroAPI.DownloadProgress as ZoteroDownloadProgress
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import com.mickstarify.zooforzotero.ZoteroStorage.domain.NoCredentialsException
import com.mickstarify.zooforzotero.ZoteroStorage.domain.ZoteroRepository
import com.mickstarify.zooforzotero.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AttachmentManagerViewModel"

data class AttachmentStats(
    val localCount: Int,
    val totalCount: Int,
    val localSize: String
)

data class DownloadProgress(
    val current: Int = 0,
    val total: Int = 0,
    val fileName: String = "",
    val isIndeterminate: Boolean = true
)

@HiltViewModel
class AttachmentManagerViewModel @Inject constructor(
    private val zoteroRepository: ZoteroRepository,
    private val calculateAttachmentMetadataUseCase: CalculateAttachmentMetadataUseCase
) : BaseViewModel<AttachmentManagerViewModel.State, AttachmentManagerViewModel.Effect, AttachmentManagerViewModel.Event>() {

    private val _stateFlow = MutableStateFlow<State>(State.InitialState)
    override val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()
    
    private var currentZoteroDB: com.mickstarify.zooforzotero.ZoteroStorage.ZoteroDB.ZoteroDB? = null
    private var downloadJob: Job? = null

    sealed class State {
        object InitialState : State()
        
        object LoadingState : State()
        
        data class LoadedState(
            val attachmentStats: AttachmentStats? = null,
            val downloadProgress: DownloadProgress = DownloadProgress(),
            val isDownloading: Boolean = false,
            val downloadButtonText: String = "Download All Attachments",
            val downloadButtonEnabled: Boolean = false,
            val error: String? = null
        ) : State()
    }

    sealed class Effect {
        object NavigateBack : Effect()
        data class ShowError(val message: String) : Effect()
        data class OpenAttachment(val attachmentPath: String) : Effect()
    }

    sealed class Event {
        object LoadLibrary : Event()
        object DownloadButtonPressed : Event()
        object CancelDownload : Event()
        object NavigateBack : Event()
    }


    override fun handleEvent(event: Event) {
        when (event) {
            is Event.LoadLibrary -> {
                loadLibraryAndCalculateMetadata()
            }

            is Event.DownloadButtonPressed -> {
                startDownloadAllAttachments()
            }

            is Event.CancelDownload -> {
                cancelDownload()
            }

            is Event.NavigateBack -> {
                viewModelScope.launch {
                    triggerEffect(Effect.NavigateBack)
                }
            }
        }
    }

    private fun loadLibraryAndCalculateMetadata() {
        viewModelScope.launch {
            _stateFlow.value = State.LoadingState

            // Load library from database
            zoteroRepository.loadLibrary(GroupInfo.NO_GROUP_ID)
                .onSuccess { zoteroDB ->
                    currentZoteroDB = zoteroDB
                    Log.d(TAG, "Library loaded successfully")
                    
                    // Calculate attachment metadata
                    calculateAttachmentMetadataUseCase.execute(zoteroDB)
                        .catch { exception ->
                            Log.e(TAG, "Error calculating metadata", exception)
                            _stateFlow.value = State.LoadedState(
                                error = "Error reading filesystem: ${exception.message}",
                                downloadButtonText = "Download All Attachments",
                                downloadButtonEnabled = false
                            )
                        }
                        .collect { progress ->
                            val sizeString = formatFileSize(progress.totalSizeBytes)
                            val stats = AttachmentStats(
                                localCount = progress.localAttachmentCount,
                                totalCount = progress.totalAttachmentCount,
                                localSize = sizeString
                            )
                            
                            _stateFlow.value = State.LoadedState(
                                attachmentStats = stats,
                                downloadButtonText = if (progress.isComplete) "Download All Attachments" else "Loading...",
                                downloadButtonEnabled = progress.isComplete
                            )
                            
                            if (progress.isComplete) {
                                Log.d(TAG, "Metadata calculation complete: ${progress.localAttachmentCount}/${progress.totalAttachmentCount} files")
                            }
                        }
                }
                .onFailure { exception ->
                    Log.e(TAG, "Error loading library", exception)
                    val errorMessage = when (exception) {
                        is NoCredentialsException -> "No credentials available. Please re-authenticate."
                        else -> "Error loading library: ${exception.message}"
                    }
                    
                    _stateFlow.value = State.LoadedState(
                        error = errorMessage,
                        downloadButtonText = "Download All Attachments",
                        downloadButtonEnabled = false
                    )
                    
                    viewModelScope.launch {
                        triggerEffect(Effect.ShowError(errorMessage))
                    }
                }
        }
    }

    private fun startDownloadAllAttachments() {
        val zoteroDB = currentZoteroDB
        if (zoteroDB == null) {
            Log.e(TAG, "Cannot download attachments: library not loaded")
            return
        }
        
        // Cancel any existing download
        downloadJob?.cancel()
        
        downloadJob = viewModelScope.launch {
            try {
                // Get all downloadable attachments
                val attachmentItems = zoteroDB.items?.filter { item ->
                    item.itemType == "attachment" && 
                    item.isDownloadable() && 
                    item.data["linkMode"] != "linked_file"
                } ?: emptyList()
                
                if (attachmentItems.isEmpty()) {
                    Log.d(TAG, "No attachments to download")
                    triggerEffect(Effect.ShowError("No attachments to download"))
                    return@launch
                }
                
                Log.d(TAG, "Starting download of ${attachmentItems.size} attachments")
                
                // Update state to show downloading
                _stateFlow.value = State.LoadedState(
                    attachmentStats = (_stateFlow.value as? State.LoadedState)?.attachmentStats,
                    isDownloading = true,
                    downloadButtonText = "Downloading...",
                    downloadButtonEnabled = false,
                    downloadProgress = DownloadProgress(current = 0, total = attachmentItems.size, isIndeterminate = false)
                )
                
                var successCount = 0
                var failureCount = 0
                
                attachmentItems.forEachIndexed { index, item ->
                    try {
                        Log.d(TAG, "Downloading attachment ${index + 1}/${attachmentItems.size}: ${item.getTitle()}")
                        
                        // Update progress
                        _stateFlow.value = State.LoadedState(
                            attachmentStats = (_stateFlow.value as? State.LoadedState)?.attachmentStats,
                            isDownloading = true,
                            downloadButtonText = "Downloading...",
                            downloadButtonEnabled = false,
                            downloadProgress = DownloadProgress(
                                current = index + 1,
                                total = attachmentItems.size,
                                fileName = item.data["filename"] ?: item.getTitle(),
                                isIndeterminate = false
                            )
                        )
                        
                        // Download the individual attachment
                        zoteroRepository.downloadAttachment(item, zoteroDB.groupID)
                            .catch { exception ->
                                Log.e(TAG, "Error downloading ${item.getTitle()}: ${exception.message}", exception)
                                failureCount++
                            }
                            .collect { downloadProgress ->
                                // Individual download progress - could be used for more detailed progress
                                Log.d(TAG, "Download progress for ${item.getTitle()}: ${downloadProgress.progress}/${downloadProgress.total}")
                            }
                        
                        successCount++
                        
                    } catch (exception: Exception) {
                        Log.e(TAG, "Error downloading ${item.getTitle()}: ${exception.message}", exception)
                        failureCount++
                    }
                }
                
                // Download completed - recalculate metadata
                Log.d(TAG, "Download completed: $successCount successful, $failureCount failed")
                
                val currentState = _stateFlow.value as? State.LoadedState
                _stateFlow.value = State.LoadedState(
                    attachmentStats = currentState?.attachmentStats,
                    isDownloading = false,
                    downloadButtonText = "Download All Attachments",
                    downloadButtonEnabled = true,
                    downloadProgress = DownloadProgress()
                )
                
                // Refresh metadata after download
                loadLibraryAndCalculateMetadata()
                
                // Show completion message
                val message = if (failureCount > 0) {
                    "Download completed: $successCount successful, $failureCount failed"
                } else {
                    "All attachments downloaded successfully!"
                }
                triggerEffect(Effect.ShowError(message))
                
            } catch (exception: Exception) {
                Log.e(TAG, "Error during download process", exception)
                
                val currentState = _stateFlow.value as? State.LoadedState
                _stateFlow.value = State.LoadedState(
                    attachmentStats = currentState?.attachmentStats,
                    isDownloading = false,
                    downloadButtonText = "Download All Attachments",
                    downloadButtonEnabled = true,
                    error = "Download failed: ${exception.message}",
                    downloadProgress = DownloadProgress()
                )
                
                triggerEffect(Effect.ShowError("Download failed: ${exception.message}"))
            }
        }
    }

    private fun cancelDownload() {
        Log.d(TAG, "Canceling download")
        downloadJob?.cancel()
        downloadJob = null
        
        val currentState = _stateFlow.value as? State.LoadedState
        _stateFlow.value = State.LoadedState(
            attachmentStats = currentState?.attachmentStats,
            isDownloading = false,
            downloadButtonText = "Download All Attachments",
            downloadButtonEnabled = true,
            downloadProgress = DownloadProgress()
        )
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1000L -> "${bytes}B"
            bytes < 1000000L -> "${(bytes / 1000L).toInt()}KB"
            else -> {
                val mb = bytes.toDouble() / 1000000.0
                "%.2fMB".format(mb)
            }
        }
    }
    
}