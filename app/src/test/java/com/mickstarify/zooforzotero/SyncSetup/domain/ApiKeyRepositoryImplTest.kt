package com.mickstarify.zooforzotero.SyncSetup.domain

import com.mickstarify.zooforzotero.ZoteroAPI.Model.KeyInfo
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroAPIService
import com.mickstarify.zooforzotero.common.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ApiKeyRepositoryImplTest : BaseViewModelTest() {

    private lateinit var repository: ApiKeyRepositoryImpl
    private val mockApiService = mock<ZoteroAPIService>()
    private val mockCall = mock<Call<KeyInfo>>()

    companion object {
        private const val VALID_API_KEY = "valid_test_key_123"
        private const val INVALID_API_KEY = "invalid_key_456"
        private const val TEST_USERNAME = "testuser"
        private const val TEST_USER_ID = 12345
        
        private val TEST_KEY_INFO = KeyInfo(
            key = VALID_API_KEY,
            userID = TEST_USER_ID,
            username = TEST_USERNAME
        )
    }

    @BeforeEach
    override fun setup() {
        repository = ApiKeyRepositoryImpl(dispatchersProvider, mockApiService)
    }

    @Test
    fun `validateApiKey returns Success when API returns 200 with valid KeyInfo`() = runTest(testDispatcher) {
        // Arrange
        whenever(mockCall.execute()).thenReturn(Response.success(TEST_KEY_INFO))
        whenever(mockApiService.getKeyInfo(VALID_API_KEY)).thenReturn(mockCall)
        
        // Act
        val result = repository.validateApiKey(VALID_API_KEY)
        
        // Assert
        assertTrue(result is ApiKeyRepository.ValidationResult.Success)
        val successResult = result as ApiKeyRepository.ValidationResult.Success
        assertEquals(TEST_KEY_INFO, successResult.keyInfo)
        assertEquals(VALID_API_KEY, successResult.keyInfo.key)
        assertEquals(TEST_USERNAME, successResult.keyInfo.username)
        assertEquals(TEST_USER_ID, successResult.keyInfo.userID)
    }

    @Test
    fun `validateApiKey returns Error when API returns 200 with null body`() = runTest(testDispatcher) {
        // Arrange
        whenever(mockCall.execute()).thenReturn(Response.success(null))
        whenever(mockApiService.getKeyInfo(VALID_API_KEY)).thenReturn(mockCall)
        
        // Act
        val result = repository.validateApiKey(VALID_API_KEY)
        
        // Assert
        assertTrue(result is ApiKeyRepository.ValidationResult.Error)
        val errorResult = result as ApiKeyRepository.ValidationResult.Error
        assertEquals("Error, got back unrecognizable data from Zotero API.", errorResult.message)
    }

    @Test
    fun `validateApiKey returns Error when API returns 404`() = runTest(testDispatcher) {
        // Arrange
        val response = Response.error<KeyInfo>(404, mock())
        whenever(mockCall.execute()).thenReturn(response)
        whenever(mockApiService.getKeyInfo(INVALID_API_KEY)).thenReturn(mockCall)
        
        // Act
        val result = repository.validateApiKey(INVALID_API_KEY)
        
        // Assert
        assertTrue(result is ApiKeyRepository.ValidationResult.Error)
        val errorResult = result as ApiKeyRepository.ValidationResult.Error
        assertEquals("Error: Your API key was not found. Please check your key and try again.", errorResult.message)
    }

    @Test
    fun `validateApiKey returns Error when API returns 401 Unauthorized`() = runTest(testDispatcher) {
        // Arrange
        val response = Response.error<KeyInfo>(401, mock())
        whenever(mockCall.execute()).thenReturn(response)
        whenever(mockApiService.getKeyInfo(INVALID_API_KEY)).thenReturn(mockCall)
        
        // Act
        val result = repository.validateApiKey(INVALID_API_KEY)
        
        // Assert
        assertTrue(result is ApiKeyRepository.ValidationResult.Error)
        val errorResult = result as ApiKeyRepository.ValidationResult.Error
        assertEquals("Unknown network error, got back server code 401", errorResult.message)
    }

    @Test
    fun `validateApiKey returns Error when API returns 500 Internal Server Error`() = runTest(testDispatcher) {
        // Arrange
        val response = Response.error<KeyInfo>(500, mock())
        whenever(mockCall.execute()).thenReturn(response)
        whenever(mockApiService.getKeyInfo(VALID_API_KEY)).thenReturn(mockCall)
        
        // Act
        val result = repository.validateApiKey(VALID_API_KEY)
        
        // Assert
        assertTrue(result is ApiKeyRepository.ValidationResult.Error)
        val errorResult = result as ApiKeyRepository.ValidationResult.Error
        assertEquals("Unknown network error, got back server code 500", errorResult.message)
    }

    @Test
    fun `validateApiKey returns NetworkError when IOException is thrown`() = runTest(testDispatcher) {
        // Arrange
        whenever(mockCall.execute()).thenThrow(IOException("Network error"))
        whenever(mockApiService.getKeyInfo(VALID_API_KEY)).thenReturn(mockCall)
        
        // Act
        val result = repository.validateApiKey(VALID_API_KEY)
        
        // Assert
        assertTrue(result is ApiKeyRepository.ValidationResult.NetworkError)
    }

    @Test
    fun `validateApiKey returns NetworkError when RuntimeException is thrown`() = runTest(testDispatcher) {
        // Arrange
        whenever(mockCall.execute()).thenThrow(RuntimeException("Unexpected error"))
        whenever(mockApiService.getKeyInfo(VALID_API_KEY)).thenReturn(mockCall)
        
        // Act
        val result = repository.validateApiKey(VALID_API_KEY)
        
        // Assert
        assertTrue(result is ApiKeyRepository.ValidationResult.NetworkError)
    }

    @Test
    fun `validateApiKey handles empty API key string`() = runTest(testDispatcher) {
        // Arrange
        val emptyApiKey = ""
        val response = Response.error<KeyInfo>(404, mock())
        whenever(mockCall.execute()).thenReturn(response)
        whenever(mockApiService.getKeyInfo(emptyApiKey)).thenReturn(mockCall)
        
        // Act
        val result = repository.validateApiKey(emptyApiKey)
        
        // Assert
        assertTrue(result is ApiKeyRepository.ValidationResult.Error)
        val errorResult = result as ApiKeyRepository.ValidationResult.Error
        assertEquals("Error: Your API key was not found. Please check your key and try again.", errorResult.message)
    }

    @Test
    fun `validateApiKey handles special characters in API key`() = runTest(testDispatcher) {
        // Arrange
        val specialApiKey = "test-key_with.special@chars"
        whenever(mockCall.execute()).thenReturn(Response.success(TEST_KEY_INFO.copy(key = specialApiKey)))
        whenever(mockApiService.getKeyInfo(specialApiKey)).thenReturn(mockCall)
        
        // Act
        val result = repository.validateApiKey(specialApiKey)
        
        // Assert
        assertTrue(result is ApiKeyRepository.ValidationResult.Success)
        val successResult = result as ApiKeyRepository.ValidationResult.Success
        assertEquals(specialApiKey, successResult.keyInfo.key)
    }
}