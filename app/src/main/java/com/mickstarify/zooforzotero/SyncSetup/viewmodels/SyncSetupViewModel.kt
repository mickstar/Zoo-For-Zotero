package com.mickstarify.zooforzotero.SyncSetup.viewmodels

import androidx.lifecycle.viewModelScope
import com.mickstarify.zooforzotero.SyncSetup.SyncOption
import com.mickstarify.zooforzotero.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncSetupViewModel @Inject constructor(
) : BaseViewModel<SyncSetupViewModel.State, SyncSetupViewModel.Effect, SyncSetupViewModel.Event>() {

    data class State(
        val selectedSyncOption: SyncOption = SyncOption.Unset,
        val isProceedEnabled: Boolean = false,
        val showApiKeyDialog: Boolean = false
    )

    sealed class Effect {
        object NavigateToZoteroApiSetup : Effect()
        object NavigateToLibrary : Effect()
    }

    sealed class Event {
        data class SelectSyncOption(val option: SyncOption) : Event()
        object ProceedWithSetup : Event()
        object DismissApiKeyDialog : Event()
        data class SubmitApiKey(val apiKey: String) : Event()
    }

    override fun getInitialState(): State = State()

    override fun handleEvent(event: Event) {
        when (event) {
            is Event.SelectSyncOption -> {
                updateState { 
                    it.copy(
                        selectedSyncOption = event.option,
                        isProceedEnabled = event.option != SyncOption.Unset
                    )
                }
            }
            
            is Event.ProceedWithSetup -> {
                when (currentState.selectedSyncOption) {
                    SyncOption.ZoteroAPI -> {
                        viewModelScope.launch {
                            sendEffect(Effect.NavigateToZoteroApiSetup)
                        }
                    }
                    SyncOption.ZoteroAPIManual -> {
                        updateState { it.copy(showApiKeyDialog = true) }
                    }
                    SyncOption.Unset -> {
                        // Do nothing - button should be disabled
                    }
                }
            }
            
            is Event.DismissApiKeyDialog -> {
                updateState { it.copy(showApiKeyDialog = false) }
            }
            
            is Event.SubmitApiKey -> {
                updateState { it.copy(showApiKeyDialog = false) }
                // TODO: Validate API key, for now just navigate to library
                viewModelScope.launch {
                    sendEffect(Effect.NavigateToLibrary)
                }
            }
        }
    }
}