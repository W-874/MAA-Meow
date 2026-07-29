package com.aliothmoon.maameow.presentation.viewmodel

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.MotionEvent
import com.aliothmoon.maameow.ITouchEventCallback
import com.aliothmoon.maameow.manager.RemoteServiceManager
import com.aliothmoon.maameow.presentation.state.PreviewTouchMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

fun interface TouchFrameScheduler {
    fun schedule(callback: () -> Unit)
}

private object ChoreographerTouchFrameScheduler : TouchFrameScheduler {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun schedule(callback: () -> Unit) {
        val post = {
            Choreographer.getInstance().postFrameCallback { callback() }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) post() else mainHandler.post(post)
    }
}

class TouchPreviewController(
    private val scope: CoroutineScope,
    private val frameScheduler: TouchFrameScheduler = ChoreographerTouchFrameScheduler,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _markers = MutableStateFlow<List<PreviewTouchMarker>>(emptyList())
    val markers: StateFlow<List<PreviewTouchMarker>> = _markers.asStateFlow()

    private val markerId = AtomicLong(0L)
    private var cleanupJob: Job? = null
    private val pendingMove = AtomicReference<PreviewTouchMarker?>()
    private val frameScheduled = AtomicBoolean(false)
    private val eventLock = Any()

    val callback = lazy {
        object : ITouchEventCallback.Stub() {
            override fun onCallback(x: Int, y: Int, type: Int) {
                acceptTouchEvent(x, y, type)
            }
        }
    }

    internal fun acceptTouchEvent(x: Int, y: Int, type: Int) {
        if (type != MotionEvent.ACTION_DOWN &&
            type != MotionEvent.ACTION_MOVE &&
            type != MotionEvent.ACTION_UP
        ) return

        val marker = PreviewTouchMarker(
            id = markerId.incrementAndGet(),
            x = x,
            y = y,
            action = type,
            createdAtMs = elapsedRealtime(),
        )
        synchronized(eventLock) {
            if (type == MotionEvent.ACTION_MOVE) {
                pendingMove.set(marker)
                scheduleMoveFlush()
            } else {
                flushPendingMoveLocked()
                appendMarker(marker)
            }
        }
        ensureCleanupJob()
    }

    fun onTouchCallbackChange(enabled: Boolean) {
        val service = RemoteServiceManager.getInstanceOrNull() ?: return
        if (enabled) {
            scope.launch(Dispatchers.IO) {
                runCatching { service.setTouchCallback(callback.value) }
            }
        } else if (callback.isInitialized()) {
            scope.launch(Dispatchers.IO) {
                runCatching { service.setTouchCallback(null) }
            }
            onClear()
        }
    }

    fun onClear() {
        cleanupJob?.cancel()
        cleanupJob = null
        synchronized(eventLock) {
            pendingMove.set(null)
            frameScheduled.set(false)
            _markers.value = emptyList()
        }
    }

    private fun scheduleMoveFlush() {
        if (!frameScheduled.compareAndSet(false, true)) return
        frameScheduler.schedule {
            synchronized(eventLock) {
                flushPendingMoveLocked()
                frameScheduled.set(false)
                if (pendingMove.get() != null) scheduleMoveFlush()
            }
        }
    }

    private fun flushPendingMoveLocked() {
        pendingMove.getAndSet(null)?.let(::appendMarker)
    }

    private fun appendMarker(marker: PreviewTouchMarker) {
        _markers.update { current ->
            val max = PreviewTouchMarker.MAX_ACTIVE_MARKERS
            buildList(max) {
                val start = if (current.size >= max) current.size - max + 1 else 0
                for (index in start until current.size) add(current[index])
                add(marker)
            }
        }
    }

    private fun ensureCleanupJob() {
        if (cleanupJob?.isActive == true) return
        cleanupJob = scope.launch {
            while (true) {
                delay(PreviewTouchMarker.CLEANUP_INTERVAL_MS)
                val cutoff = elapsedRealtime() - PreviewTouchMarker.TTL_MS
                _markers.update { markers -> markers.filter { it.createdAtMs > cutoff } }
                if (_markers.value.isEmpty()) break
            }
        }.also { activeJob ->
            activeJob.invokeOnCompletion {
                if (cleanupJob === activeJob) cleanupJob = null
            }
        }
    }
}
