package com.mickstarify.zooforzotero.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestDispatcher

class TestDispatchersProvider(
    private val testDispatcher: TestDispatcher
) : DispatchersProvider {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}