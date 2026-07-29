package com.aliothmoon.maameow

import android.app.Application
import android.os.Looper
import com.aliothmoon.maameow.data.datasource.AppDownloader
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.manager.RemoteServiceManager
import com.aliothmoon.maameow.overlay.OverlayController
import com.aliothmoon.maameow.schedule.data.ScheduleStrategyRepository
import com.aliothmoon.maameow.schedule.service.ScheduleAlarmManager
import com.aliothmoon.maameow.utils.CrashHandler
import com.aliothmoon.maameow.utils.PerformanceTrace
import com.aliothmoon.maameow.utils.SuspendOnce
import com.aliothmoon.maameow.utils.i18n.LocaleBootstrap
import com.aliothmoon.maameow.utils.log.LogTreeHolder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.withStateAtLeast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.context.GlobalContext
import kotlin.coroutines.resume

class AppInitializationCoordinator(
    private val application: Application,
    private val appSettings: AppSettingsManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val settingsInitialization = SuspendOnce()
    private val remoteInitialization = SuspendOnce()
    private val deferredInitialization = SuspendOnce()
    private val firstInteractive = MutableStateFlow(false)

    suspend fun ensureReady() {
        ensureMinimalReady()
        remoteInitialization.run {
            withContext(Dispatchers.Main.immediate) {
                PerformanceTrace.section("AppInit.remote") {
                    RemoteServiceManager.initialize(application, appSettings)
                }
            }
        }
    }

    suspend fun ensureMinimalReady() {
        settingsInitialization.run {
            appSettings.awaitLoaded()
            withContext(Dispatchers.Main.immediate) {
                PerformanceTrace.section("AppInit.locale") {
                    LocaleBootstrap.applyPersisted(appSettings)
                }
            }
        }
    }

    fun startAfterFirstInteractive() {
        firstInteractive.value = true
        scope.launch {
            deferredInitialization.run {
                awaitMainQueueIdle()
                ensureReady()
                val trace = PerformanceTrace.beginAsync("AppInit.deferred")
                try {
                    val koin = GlobalContext.get()
                    koin.get<LogTreeHolder>().setup()
                    koin.get<CrashHandler>().cleanupOldCrashLogs()

                    if (appSettings.runMode.value == RunMode.FOREGROUND) {
                        awaitMainQueueIdle()
                        withContext(Dispatchers.Main.immediate) {
                            koin.get<OverlayController>().setup()
                        }
                    }

                    koin.get<AppDownloader>().cleanInstalledApks()

                    val repository = koin.get<ScheduleStrategyRepository>()
                    repository.isLoaded.filter { it }.first()
                    koin.get<ScheduleAlarmManager>().rescheduleAll(repository.strategies.value)
                } finally {
                    PerformanceTrace.endAsync("AppInit.deferred", trace)
                }
            }
        }
    }

    suspend fun awaitUpdateWindow() {
        appSettings.awaitLoaded()
        awaitFirstInteractive()
        ProcessLifecycleOwner.get().lifecycle.withStateAtLeast(Lifecycle.State.RESUMED) {}
    }

    suspend fun awaitFirstInteractive() {
        firstInteractive.first { it }
    }

    private suspend fun awaitMainQueueIdle() = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val queue = Looper.myQueue()
            val idleHandler = android.os.MessageQueue.IdleHandler {
                if (continuation.isActive) continuation.resume(Unit)
                false
            }
            queue.addIdleHandler(idleHandler)
            continuation.invokeOnCancellation { queue.removeIdleHandler(idleHandler) }
        }
    }
}
