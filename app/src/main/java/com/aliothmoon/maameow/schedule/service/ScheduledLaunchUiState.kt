package com.aliothmoon.maameow.schedule.service

import com.aliothmoon.maameow.schedule.model.CountdownState
import com.aliothmoon.maameow.schedule.model.ScheduledExecutionRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ScheduledLaunchInbox {
    private val _pending = MutableStateFlow<ScheduledExecutionRequest?>(null)
    val pending: StateFlow<ScheduledExecutionRequest?> = _pending.asStateFlow()

    fun submit(request: ScheduledExecutionRequest) {
        _pending.value = request
    }

    fun consume(requestId: String): ScheduledExecutionRequest? {
        val request = _pending.value?.takeIf { it.requestId == requestId } ?: return null
        _pending.value = null
        return request
    }
}

class ScheduledLaunchUiState {
    @Volatile
    private var cancelAction: (() -> Unit)? = null
    @Volatile
    private var startNowAction: (() -> Unit)? = null
    private val _countdown = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val countdown: StateFlow<CountdownState> = _countdown.asStateFlow()

    private val _pendingExecution = MutableStateFlow<ScheduledExecutionRequest?>(null)
    val pendingExecution: StateFlow<ScheduledExecutionRequest?> = _pendingExecution.asStateFlow()

    private val _feedbackMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val feedbackMessages: SharedFlow<String> = _feedbackMessages.asSharedFlow()

    internal fun setCountdown(state: CountdownState) {
        _countdown.value = state
    }

    internal fun setPendingExecution(request: ScheduledExecutionRequest?) {
        _pendingExecution.value = request
    }

    internal fun emitFeedback(message: String) {
        _feedbackMessages.tryEmit(message)
    }

    internal fun attachActions(onCancel: () -> Unit, onStartNow: () -> Unit) {
        cancelAction = onCancel
        startNowAction = onStartNow
    }

    fun cancel() {
        cancelAction?.invoke()
    }

    fun startNow() {
        startNowAction?.invoke()
    }
}
