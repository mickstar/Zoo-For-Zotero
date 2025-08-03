package com.mickstarify.zooforzotero.AttachmentManager.viewmodel

import androidx.lifecycle.viewModelScope
import com.mickstarify.zooforzotero.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
) : BaseViewModel<AttachmentManagerViewModel.State, AttachmentManagerViewModel.Effect, AttachmentManagerViewModel.Event>() {

    private val _stateFlow = MutableStateFlow(State())
    override val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()

    data class State(
        val isLoading: Boolean = false,
        val attachments: List<String> = emptyList(),
        val selectedAttachment: String? = null,
        val error: String? = null,
        val attachmentStats: AttachmentStats? = null,
        val downloadProgress: DownloadProgress = DownloadProgress(),
        val isDownloading: Boolean = false
    )

    sealed class Effect {
        object NavigateBack : Effect()
        data class ShowError(val message: String) : Effect()
        data class OpenAttachment(val attachmentPath: String) : Effect()
    }

    sealed class Event {
        object LoadAttachments : Event()
        data class SelectAttachment(val attachment: String) : Event()
        object RefreshAttachments : Event()
        object NavigateBack : Event()
    }

    override fun handleEvent(event: Event) {
        when (event) {
            is Event.LoadAttachments -> {
                updateCurrentState { it.copy(isLoading = true, error = null) }
                // TODO: Implement attachment loading logic
            }

            is Event.SelectAttachment -> {
                updateCurrentState { it.copy(selectedAttachment = event.attachment) }
                viewModelScope.launch {
                    triggerEffect(Effect.OpenAttachment(event.attachment))
                }
            }

            is Event.RefreshAttachments -> {
                updateCurrentState { it.copy(isLoading = true, error = null) }
                // TODO: Implement refresh logic
            }

            is Event.NavigateBack -> {
                viewModelScope.launch {
                    triggerEffect(Effect.NavigateBack)
                }
            }
        }
    }
    
    private fun updateCurrentState(transform: (State) -> State) {
        _stateFlow.value = transform(_stateFlow.value)
    }
}