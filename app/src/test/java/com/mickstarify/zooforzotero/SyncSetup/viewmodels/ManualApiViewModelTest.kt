package com.mickstarify.zooforzotero.SyncSetup.viewmodels

import app.cash.turbine.test
import com.mickstarify.zooforzotero.SyncSetup.domain.ApiKeyRepository
import com.mickstarify.zooforzotero.SyncSetup.domain.AuthenticationStorage
import com.mickstarify.zooforzotero.ZoteroAPI.Model.KeyInfo
import com.mickstarify.zooforzotero.common.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ManualApiViewModelTest : BaseViewModelTest() {

    companion object {
        private const val TEST_API_KEY = "test-api-key"
        private const val VALID_API_KEY = "valid-api-key"
        private const val INVALID_API_KEY = "invalid-api-key"
        private const val TEST_USERNAME = "testuser"
        private const val TEST_USER_ID = 12345
        private const val TEST_USER_ID_STRING = "12345"
        private const val INVALID_API_ERROR_MESSAGE = "Invalid API key"
        private const val NETWORK_ERROR_MESSAGE = "Network error connecting to Zotero API."
        private const val BLANK_KEY_INPUT = "   "
        private const val EMPTY_STRING = ""
    }

    private val authStorage: AuthenticationStorage = mock()
    private val apiKeyRepository: ApiKeyRepository = mock()
    private lateinit var viewModel: ManualApiViewModel

    override fun setup() {
        viewModel = ManualApiViewModel(
            apiKeyRepository,
            authStorage
        )
    }

    @Test
    fun `initial state should have empty api key and submit disabled`() = runTest {
        viewModel.state.test {
            val initialState = awaitItem()
            assertEquals(ManualApiViewModel.State(
                apiKey = EMPTY_STRING,
                isValidating = false,
                errorMessage = null,
                isSubmitEnabled = false
            ), initialState)
        }
    }

    @Test
    fun `api key changed event should update state and enable submit`() = runTest {
        viewModel.state.test {
            val initialState = awaitItem()
            assertEquals(ManualApiViewModel.State(
                apiKey = EMPTY_STRING,
                isValidating = false,
                errorMessage = null,
                isSubmitEnabled = false
            ), initialState)

            viewModel.dispatch(ManualApiViewModel.Event.ApiKeyChanged(TEST_API_KEY))

            val updatedState = awaitItem()
            assertEquals(ManualApiViewModel.State(
                apiKey = TEST_API_KEY,
                isValidating = false,
                errorMessage = null,
                isSubmitEnabled = true
            ), updatedState)
        }
    }

    @Test
    fun `clear error event should remove error message`() = runTest {
        // Set up a mock that will return an error to create an error state
        whenever(apiKeyRepository.validateApiKey(TEST_API_KEY))
            .thenReturn(ApiKeyRepository.ValidationResult.Error("Test error"))

        viewModel.state.test {
            awaitItem() // Initial state
            
            // Set API key and submit to create an error
            viewModel.dispatch(ManualApiViewModel.Event.ApiKeyChanged(TEST_API_KEY))
            awaitItem() // Key changed state
            
            viewModel.dispatch(ManualApiViewModel.Event.SubmitApiKey)
            awaitItem() // Validating state
            
            val errorState = awaitItem() // Error state
            assertEquals(ManualApiViewModel.State(
                apiKey = TEST_API_KEY,
                isValidating = false,
                errorMessage = "Test error",
                isSubmitEnabled = true
            ), errorState)
            
            // Now clear the error
            viewModel.dispatch(ManualApiViewModel.Event.ClearError)

            val clearedState = awaitItem()
            assertEquals(ManualApiViewModel.State(
                apiKey = TEST_API_KEY,
                isValidating = false,
                errorMessage = null,
                isSubmitEnabled = true
            ), clearedState)
        }
    }


    @Test
    fun `successful api key validation should save credentials and navigate to library`() =
        runTest {
            val keyInfo = KeyInfo(
                username = TEST_USERNAME,
                userID = TEST_USER_ID,
                key = VALID_API_KEY
            )
            whenever(apiKeyRepository.validateApiKey(VALID_API_KEY))
                .thenReturn(ApiKeyRepository.ValidationResult.Success(keyInfo))

            // Set API key first

            // Test state changes first
            viewModel.state.test {
                val initialState = awaitItem()
                viewModel.dispatch(ManualApiViewModel.Event.ApiKeyChanged(VALID_API_KEY))

                val changedState = awaitItem()

                viewModel.dispatch(ManualApiViewModel.Event.SubmitApiKey)

                // Should see validating true
                val validatingState = awaitItem()
                assertEquals(ManualApiViewModel.State(
                    apiKey = VALID_API_KEY,
                    isSubmitEnabled = true,
                    isValidating = true,
                    errorMessage = null
                ), validatingState)

            }

            // Test effects separately (effect was already dispatched above)
            viewModel.effects.test {
                val effect = awaitItem()
                assertEquals(ManualApiViewModel.Effect.NavigateToLibrary, effect)
            }

            verify(authStorage).setCredentials(TEST_USERNAME, TEST_USER_ID_STRING, VALID_API_KEY)
        }

    @Test
    fun `failed api key validation should show error message`() = runTest {
        whenever(apiKeyRepository.validateApiKey(INVALID_API_KEY))
            .thenReturn(ApiKeyRepository.ValidationResult.Error(INVALID_API_ERROR_MESSAGE))

        viewModel.state.test {
            awaitItem() // Initial state

            viewModel.dispatch(ManualApiViewModel.Event.ApiKeyChanged(INVALID_API_KEY))
            val keyChangedState = awaitItem()

            viewModel.dispatch(ManualApiViewModel.Event.SubmitApiKey)

            val validatingState = awaitItem()
            assertEquals(ManualApiViewModel.State(
                apiKey = INVALID_API_KEY,
                isValidating = true,
                errorMessage = null,
                isSubmitEnabled = true
            ), validatingState)

            val errorState = awaitItem()
            assertEquals(ManualApiViewModel.State(
                apiKey = INVALID_API_KEY,
                isValidating = false,
                errorMessage = INVALID_API_ERROR_MESSAGE,
                isSubmitEnabled = true
            ), errorState)
        }
    }

    @Test
    fun `network error during validation should show network error message`() = runTest {
        whenever(apiKeyRepository.validateApiKey(TEST_API_KEY))
            .thenReturn(ApiKeyRepository.ValidationResult.NetworkError)

        viewModel.state.test {
            awaitItem() // Initial state

            viewModel.dispatch(ManualApiViewModel.Event.ApiKeyChanged(TEST_API_KEY))
            awaitItem() // Key changed state

            viewModel.dispatch(ManualApiViewModel.Event.SubmitApiKey)

            val validatingState = awaitItem()
            assertEquals(ManualApiViewModel.State(
                apiKey = TEST_API_KEY,
                isValidating = true,
                errorMessage = null,
                isSubmitEnabled = true
            ), validatingState)

            val errorState = awaitItem()
            assertEquals(ManualApiViewModel.State(
                apiKey = TEST_API_KEY,
                isValidating = false,
                errorMessage = NETWORK_ERROR_MESSAGE,
                isSubmitEnabled = true
            ), errorState)
        }
    }

    @Test
    fun `navigate back event should send navigate back effect`() = runTest {
        viewModel.effects.test {
            viewModel.dispatch(ManualApiViewModel.Event.NavigateBack)
            advanceUntilIdle()

            val effect = awaitItem()
            assertEquals(ManualApiViewModel.Effect.NavigateBack, effect)
        }
    }
}