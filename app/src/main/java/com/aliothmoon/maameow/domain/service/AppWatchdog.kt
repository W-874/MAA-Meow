package com.aliothmoon.maameow.domain.service

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.aliothmoon.maameow.constant.Packages
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.remote.AppAliveStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class AppWatchdog(
    private val chainState: TaskChainState,
    private val appAliveChecker: AppAliveChecker,
    private val appSettingsManager: AppSettingsManager,
) {
    enum class WatchdogState {
        IDLE,
        WATCHING,
        APP_DIED,
    }

    companion object {
        private const val POLL_INTERVAL_MS = 5000L

        @VisibleForTesting
        internal const val MAX_REPIN_ATTEMPTS = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(WatchdogState.IDLE)
    val state: StateFlow<WatchdogState> = _state.asStateFlow()

    private val _appDiedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val appDiedEvent: SharedFlow<String> = _appDiedEvent.asSharedFlow()

    private val _displayDriftEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val displayDriftEvent: SharedFlow<String> = _displayDriftEvent.asSharedFlow()

    private var watchJob: Job? = null
    private var driftNotified = false
    private var driftRepinAttempts = 0
    private var driftFirstSeenMs: Long = 0L

    @VisibleForTesting
    internal var clock: () -> Long = { SystemClock.elapsedRealtime() }

    fun startWatching() {
        stopWatching()
        resetDriftState()

        val clientType = chainState.clientType
        val packageName = Packages[clientType]
        if (packageName == null) {
            Timber.w(
                "AppWatchdog: cannot resolve package name for clientType=%s, skipping",
                clientType
            )
            return
        }

        Timber.i("AppWatchdog: start watching %s", packageName)
        _state.value = WatchdogState.WATCHING

        watchJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val appAliveStatus = checkAppAliveStatus(packageName)
                if (!isActive) {
                    return@launch
                }
                when (appAliveStatus) {
                    AppAliveStatus.ALIVE -> checkDisplayPinned(packageName)
                    AppAliveStatus.UNKNOWN -> {
                        Timber.w(
                            "AppWatchdog: unable to determine whether %s is alive",
                            packageName
                        )
                    }

                    AppAliveStatus.DEAD -> {
                        Timber.w("AppWatchdog: app %s is no longer alive", packageName)
                        _state.value = WatchdogState.APP_DIED
                        _appDiedEvent.tryEmit(packageName)
                        return@launch
                    }

                    else -> {
                        Timber.w(
                            "AppWatchdog: unexpected app status %s for %s",
                            appAliveStatus,
                            packageName
                        )
                    }
                }
            }
        }
    }

    fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
        resetDriftState()
        _state.value = WatchdogState.IDLE
    }

    private suspend fun checkAppAliveStatus(packageName: String): Int {
        return appAliveChecker.isAppAlive(packageName)
    }

    @VisibleForTesting
    internal suspend fun checkDisplayPinned(packageName: String) {
        val onDisplay = appAliveChecker.isAppOnBackgroundDisplay(packageName) ?: return
        if (onDisplay) {
            resetDriftState()
            return
        }

        if (!appSettingsManager.driftAutoRepinEnabled.value) {
            return
        }
        if (driftRepinAttempts >= MAX_REPIN_ATTEMPTS) {
            return
        }

        val nowMs = clock()
        val delayMs = appSettingsManager.driftAutoRepinDelaySec.value * 1000L
        if (driftFirstSeenMs == 0L) {
            driftFirstSeenMs = nowMs
            Timber.i(
                "AppWatchdog: app %s left the virtual display, will repin in %d ms (grace period)",
                packageName, delayMs
            )
            return
        }
        if (nowMs - driftFirstSeenMs < delayMs) {
            return
        }

        Timber.w(
            "AppWatchdog: app %s drifted for %d ms, trying to move it back (attempt %d/%d)",
            packageName, nowMs - driftFirstSeenMs, driftRepinAttempts + 1, MAX_REPIN_ATTEMPTS
        )
        if (appAliveChecker.moveAppToBackgroundDisplay(packageName) == true) {
            Timber.i("AppWatchdog: app %s moved back to the virtual display", packageName)
            resetDriftState()
            return
        }
        driftRepinAttempts++
        if (driftRepinAttempts >= MAX_REPIN_ATTEMPTS && !driftNotified) {
            driftNotified = true
            _displayDriftEvent.tryEmit(packageName)
        }
    }

    private fun resetDriftState() {
        driftNotified = false
        driftFirstSeenMs = 0L
        driftRepinAttempts = 0
    }
}
