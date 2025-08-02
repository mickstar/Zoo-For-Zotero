package com.mickstarify.zooforzotero.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel<STATE, EFFECT, EVENT> : ViewModel() {
    
    private val _state = MutableStateFlow(getInitialState())
    val state: StateFlow<STATE> = _state.asStateFlow()
    
    private val _effects = MutableSharedFlow<EFFECT>()
    val effects: Flow<EFFECT> = _effects.asSharedFlow()
    
    protected abstract fun getInitialState(): STATE
    
    protected abstract fun handleEvent(event: EVENT)
    
    fun dispatch(event: EVENT) {
        handleEvent(event)
    }
    
    protected fun setState(newState: STATE) {
        _state.value = newState
    }
    
    protected fun updateState(update: (STATE) -> STATE) {
        _state.value = update(_state.value)
    }
    
    protected suspend fun sendEffect(effect: EFFECT) {
        _effects.emit(effect)
    }
    
    protected val currentState: STATE
        get() = _state.value
}