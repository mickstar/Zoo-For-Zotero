package com.mickstarify.zooforzotero.SyncSetup.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mickstarify.zooforzotero.BuildConfig
import com.mickstarify.zooforzotero.SyncSetup.domain.AuthenticationStorage
import com.mickstarify.zooforzotero.common.DispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider
import javax.inject.Inject

private const val TAG = "ZoteroAccountAuthViewModel"

@HiltViewModel
class ZoteroAccountAuthViewModel @Inject constructor(
    private val authenticationStorage: AuthenticationStorage,
    private val dispatchers: DispatchersProvider
) : ViewModel() {

    data class State(
        val isLoading: Boolean = false,
        val authorizationUrl: String? = null,
        val error: String? = null,
        val isAuthComplete: Boolean = false
    )

    sealed class Event {
        object StartOAuthFlow : Event()
        data class HandleOAuthCallback(val token: String, val verifier: String) : Event()
        object DismissError : Event()
    }

    sealed class Effect {
        object NavigateToLibrary : Effect()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = MutableStateFlow<Effect?>(null)
    val effects: StateFlow<Effect?> = _effects.asStateFlow()

    // OAuth configuration
    private val REQUEST_TOKEN_ENDPOINT = "https://www.zotero.org/oauth/request?" +
            "library_access=1&" +
            "notes_access=1&" +
            "write_access=1&" +
            "all_groups=write"

    private val ACCESS_TOKEN_ENDPOINT = "https://www.zotero.org/oauth/access?" +
            "library_access=1&" +
            "notes_access=1&" +
            "write_access=1&" +
            "all_groups=write"

    private val ZOTERO_AUTHORIZE_ENDPOINT = "https://www.zotero.org/oauth/authorize?" +
            "library_access=1&" +
            "notes_access=1&" +
            "write_access=1&" +
            "all_groups=write"

    private val oAuthProvider = CommonsHttpOAuthProvider(
        REQUEST_TOKEN_ENDPOINT,
        ACCESS_TOKEN_ENDPOINT,
        ZOTERO_AUTHORIZE_ENDPOINT
    )

    private val oAuthConsumer = CommonsHttpOAuthConsumer(
        BuildConfig.zotero_api_key,
        BuildConfig.zotero_api_secret
    )

    fun handleEvent(event: Event) {
        when (event) {
            is Event.StartOAuthFlow -> startOAuthFlow()
            is Event.HandleOAuthCallback -> handleOAuthCallback(event.token, event.verifier)
            is Event.DismissError -> dismissError()
        }
    }

    private fun startOAuthFlow() {
        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val authUrl = withContext(dispatchers.io) {
                    oAuthProvider.retrieveRequestToken(
                        oAuthConsumer,
                        "zooforzotero://oauth_callback"
                    )
                }

                Log.d(TAG, "OAuth authorization URL: $authUrl")
                _state.value = _state.value.copy(
                    isLoading = false,
                    authorizationUrl = authUrl
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error starting OAuth flow: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error connecting to Zotero's server: ${e.message}"
                )
            }
        }
    }

    private fun handleOAuthCallback(token: String, verifier: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                withContext(dispatchers.io) {
                    oAuthProvider.retrieveAccessToken(oAuthConsumer, verifier)
                }

                val params = oAuthProvider.responseParameters
                val username = params.getFirst("username")
                val userID = params.getFirst("userID")
                val userKey = oAuthConsumer.token

                Log.d(TAG, "OAuth success - userID: $userID, username: $username")

                authenticationStorage.setCredentials(username, userID, userKey)

                _state.value = _state.value.copy(
                    isLoading = false,
                    isAuthComplete = true
                )

                _effects.value = Effect.NavigateToLibrary

            } catch (e: Exception) {
                Log.e(TAG, "Error handling OAuth callback: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error completing authentication: ${e.message}"
                )
            }
        }
    }

    private fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }
}