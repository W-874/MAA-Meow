package com.aliothmoon.maameow.domain.state

import com.aliothmoon.maameow.domain.service.MaaResourceLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MaaExecutionStateStore {
    private val _state = MutableStateFlow(MaaExecutionState.IDLE)
    val state: StateFlow<MaaExecutionState> = _state.asStateFlow()

    fun set(state: MaaExecutionState) {
        _state.value = state
    }
}

class MaaResourceLoadStateStore {
    private val _state = MutableStateFlow<MaaResourceLoader.State>(MaaResourceLoader.State.NotLoaded)
    val state: StateFlow<MaaResourceLoader.State> = _state.asStateFlow()

    fun set(state: MaaResourceLoader.State) {
        _state.value = state
    }
}

class OverlayStateStore {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    fun setActive(active: Boolean) {
        _active.value = active
    }
}
