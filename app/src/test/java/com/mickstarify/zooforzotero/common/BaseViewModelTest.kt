package com.mickstarify.zooforzotero.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseViewModelTest {
    
    protected val testDispatcher = StandardTestDispatcher()
    protected val dispatchersProvider = TestDispatchersProvider(testDispatcher)
    
    @BeforeEach
    fun baseSetup() {
        Dispatchers.setMain(testDispatcher)
        setup()
    }
    
    @AfterEach
    fun baseTearDown() {
        tearDown()
        Dispatchers.resetMain()
    }
    
    protected open fun setup() {}
    
    protected open fun tearDown() {}
}