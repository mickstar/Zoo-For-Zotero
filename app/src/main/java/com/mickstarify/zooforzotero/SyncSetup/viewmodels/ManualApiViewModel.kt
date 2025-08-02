package com.mickstarify.zooforzotero.SyncSetup.viewmodels

import androidx.lifecycle.viewModelScope
import com.mickstarify.zooforzotero.SyncSetup.domain.ApiKeyRepository
import com.mickstarify.zooforzotero.SyncSetup.domain.AuthenticationStorage
import com.mickstarify.zooforzotero.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualApiViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val authStorage: AuthenticationStorage
) : BaseViewModel<ManualApiViewModel.State, ManualApiViewModel.Effect, ManualApiViewModel.Event>() {

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

    override fun getInitialState(): State = State()

    override fun handleEvent(event: Event) {
        when (event) {
            is Event.ApiKeyChanged -> {
                updateState {
                    it.copy(
                        apiKey = event.apiKey.trim(),
                        isSubmitEnabled = event.apiKey.trim().isNotBlank(),
                        errorMessage = null
                    )
                }
            }

            is Event.SubmitApiKey -> {
                if (currentState.apiKey.isNotBlank() && !currentState.isValidating) {
                    validateApiKey(currentState.apiKey)
                }
            }

            is Event.ClearError -> {
                updateState { it.copy(errorMessage = null) }
            }

            is Event.NavigateBack -> {
                viewModelScope.launch {
                    sendEffect(Effect.NavigateBack)
                }
            }
        }
    }

    private fun validateApiKey(apiKey: String) {
        updateState { it.copy(isValidating = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = apiKeyRepository.validateApiKey(apiKey)) {
                is ApiKeyRepository.ValidationResult.Success -> {
                    // Save credentials and navigate to library
                    authStorage.setCredentials(
                        result.keyInfo.username,
                        result.keyInfo.userID.toString(),
                        result.keyInfo.key
                    )
                    updateState { it.copy(isValidating = false) }
                    sendEffect(Effect.NavigateToLibrary)
                }

                is ApiKeyRepository.ValidationResult.Error -> {
                    updateState {
                        it.copy(
                            isValidating = false,
                            errorMessage = result.message
                        )
                    }
                }

                is ApiKeyRepository.ValidationResult.NetworkError -> {
                    updateState {
                        it.copy(
                            isValidating = false,
                            errorMessage = "Network error connecting to Zotero API."
                        )
                    }
                }
            }
        }
    }
}