package com.mickstarify.zooforzotero.SyncSetup.viewmodels

import androidx.lifecycle.viewModelScope
import com.mickstarify.zooforzotero.SyncSetup.domain.ApiKeyRepository
import com.mickstarify.zooforzotero.SyncSetup.domain.AuthenticationStorage
import com.mickstarify.zooforzotero.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualApiViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val authStorage: AuthenticationStorage
) : BaseViewModel<ManualApiViewModel.State, ManualApiViewModel.Effect, ManualApiViewModel.Event>() {

    private val _stateFlow = MutableStateFlow(State())
    override val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()

    data class State(
        val apiKey: String = "",
        val isValidating: Boolean = false,
        val errorMessage: String? = null,
        val isSubmitEnabled: Boolean = false
    )

    sealed class Effect {
        object NavigateToLibrary : Effect()
        object NavigateBack : Effect()
    }

    sealed class Event {
        data class ApiKeyChanged(val apiKey: String) : Event()
        object SubmitApiKey : Event()
        object ClearError : Event()
        object NavigateBack : Event()
    }


    override fun handleEvent(event: Event) {
        when (event) {
            is Event.ApiKeyChanged -> {
                updateCurrentState {
                    it.copy(
                        apiKey = event.apiKey.trim(),
                        isSubmitEnabled = event.apiKey.trim().isNotBlank(),
                        errorMessage = null
                    )
                }
            }

            is Event.SubmitApiKey -> {
                if (getCurrentState().apiKey.isNotBlank() && !getCurrentState().isValidating) {
                    validateApiKey(getCurrentState().apiKey)
                }
            }

            is Event.ClearError -> {
                updateCurrentState { it.copy(errorMessage = null) }
            }

            is Event.NavigateBack -> {
                viewModelScope.launch {
                    triggerEffect(Effect.NavigateBack)
                }
            }
        }
    }

    private fun validateApiKey(apiKey: String) {
        updateCurrentState { it.copy(isValidating = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = apiKeyRepository.validateApiKey(apiKey)) {
                is ApiKeyRepository.ValidationResult.Success -> {
                    // Save credentials and navigate to library
                    authStorage.setCredentials(
                        result.keyInfo.username,
                        result.keyInfo.userID.toString(),
                        result.keyInfo.key
                    )
                    updateCurrentState { it.copy(isValidating = false) }
                    triggerEffect(Effect.NavigateToLibrary)
                }

                is ApiKeyRepository.ValidationResult.Error -> {
                    updateCurrentState {
                        it.copy(
                            isValidating = false,
                            errorMessage = result.message
                        )
                    }
                }

                is ApiKeyRepository.ValidationResult.NetworkError -> {
                    updateCurrentState {
                        it.copy(
                            isValidating = false,
                            errorMessage = "Network error connecting to Zotero API."
                        )
                    }
                }
            }
        }
    }
    
    private fun updateCurrentState(transform: (State) -> State) {
        _stateFlow.value = transform(_stateFlow.value)
    }
    
    private fun getCurrentState(): State = _stateFlow.value
}