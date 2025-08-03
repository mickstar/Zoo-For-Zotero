package com.mickstarify.zooforzotero.SyncSetup.viewmodels

import androidx.lifecycle.viewModelScope
import com.mickstarify.zooforzotero.SyncSetup.SyncOption
import com.mickstarify.zooforzotero.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncSetupViewModel @Inject constructor(
) : BaseViewModel<SyncSetupViewModel.State, SyncSetupViewModel.Effect, SyncSetupViewModel.Event>() {

    private val _stateFlow = MutableStateFlow(State())
    override val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()

    data class State(
        val selectedSyncOption: SyncOption = SyncOption.ZoteroAPI,
        val isProceedEnabled: Boolean = true
    )

    sealed class Effect {
        object NavigateToZoteroApiSetup : Effect()
        object NavigateToApiKeyEntry : Effect()
        object NavigateToLibrary : Effect()
    }

    sealed class Event {
        data class SelectSyncOption(val option: SyncOption) : Event()
        object ProceedWithSetup : Event()
    }

    override fun handleEvent(event: Event) {
        when (event) {
            is Event.SelectSyncOption -> {
                updateCurrentState {
                    it.copy(
                        selectedSyncOption = event.option,
                        isProceedEnabled = event.option != SyncOption.Unset
                    )
                }
            }

            is Event.ProceedWithSetup -> {
                when (getCurrentState().selectedSyncOption) {
                    SyncOption.ZoteroAPI -> {
                        viewModelScope.launch {
                            triggerEffect(Effect.NavigateToZoteroApiSetup)
                        }
                    }
                    SyncOption.ZoteroAPIManual -> {
                        viewModelScope.launch {
                            triggerEffect(Effect.NavigateToApiKeyEntry)
                        }
                    }

                    SyncOption.Unset -> {}
                }
            }
        }
    }
    
    private fun updateCurrentState(transform: (State) -> State) {
        _stateFlow.value = transform(_stateFlow.value)
    }
    
    private fun getCurrentState(): State = _stateFlow.value
}