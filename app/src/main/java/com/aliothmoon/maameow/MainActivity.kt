package com.aliothmoon.maameow

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aliothmoon.maameow.data.achievement.AchievementEvents
import com.aliothmoon.maameow.data.achievement.AchievementRepository
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.service.MaaCompositionService
import com.aliothmoon.maameow.domain.service.TaskExecutionService
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.overlay.screensaver.ScreenSaverOverlayManager
import com.aliothmoon.maameow.presentation.navigation.AppNavigation
import com.aliothmoon.maameow.schedule.model.ScheduledExecutionRequest
import com.aliothmoon.maameow.schedule.service.ScheduledLaunchInbox
import com.aliothmoon.maameow.theme.MaaMeowTheme
import com.aliothmoon.maameow.utils.PerformanceTrace
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    @Volatile
    private var isUiReady: Boolean = false
    @Volatile
    private var isContentInstalled: Boolean = false

    private val appSettingsManager: AppSettingsManager by inject()
    private val achievementRepository: AchievementRepository by inject()
    private val compositionService: MaaCompositionService by inject()
    private val screenSaverManager: ScreenSaverOverlayManager by inject()
    private val scheduledLaunchInbox: ScheduledLaunchInbox by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val firstFrameTrace = PerformanceTrace.beginAsync("MaaMeow.firstFrame")
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !isContentInstalled || !isUiReady }
        super.onCreate(savedInstanceState)
        dismissTaskNotificationIfRequested(intent)
        dispatchScheduledLaunchIntent(intent)
        enableEdgeToEdge()
        lifecycleScope.launch {
            (application as MaaApplication).ensureUiBootstrapReady()
            delegate.localNightMode = appSettingsManager.themeMode.value.toAppCompatNightMode()
            installContent(firstFrameTrace)
            doObserveThemeMode()
        }
    }

    private fun installContent(firstFrameTrace: Int) {
        window.decorView.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                isUiReady = true
                val viewTreeObserver = window.decorView.viewTreeObserver
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    viewTreeObserver.registerFrameCommitCallback {
                        onFirstFrameCommitted(firstFrameTrace)
                    }
                } else {
                    window.decorView.post { onFirstFrameCommitted(firstFrameTrace) }
                }
                viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
        setContent {
            val themeMode by appSettingsManager.themeMode.collectAsStateWithLifecycle()
            val fontSizeScale by appSettingsManager.fontSizeScale.collectAsStateWithLifecycle()

            MaaMeowTheme(themeMode = themeMode) {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = baseDensity.density * fontSizeScale / 100f,
                        fontScale = baseDensity.fontScale
                    )
                ) {
                    AppNavigation()
                }
            }
        }
        isContentInstalled = true
    }

    private fun onFirstFrameCommitted(firstFrameTrace: Int) {
        PerformanceTrace.endAsync("MaaMeow.firstFrame", firstFrameTrace)
        PerformanceTrace.section("MaaMeow.firstInteractive") {
            (application as MaaApplication).startDeferredServices()
        }
        doObserveKeepScreenOn()
        lifecycleScope.launch {
            achievementRepository.report {
                event = AchievementEvents.APP_LAUNCH
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dismissTaskNotificationIfRequested(intent)
        dispatchScheduledLaunchIntent(intent)
    }

    private fun dismissTaskNotificationIfRequested(intent: Intent?) {
        if (intent?.getBooleanExtra(TaskExecutionService.EXTRA_DISMISS_ON_OPEN, false) != true) return
        getSystemService(NotificationManager::class.java)
            .cancel(TaskExecutionService.NOTIFICATION_ID)
        intent.removeExtra(TaskExecutionService.EXTRA_DISMISS_ON_OPEN)
    }

    private fun dispatchScheduledLaunchIntent(intent: Intent?) {
        val request = ScheduledExecutionRequest.fromIntent(intent)
            ?: ScheduledExecutionRequest.fromExternalIntent(intent)
        request?.let(scheduledLaunchInbox::submit)
    }

    private fun doObserveKeepScreenOn() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    compositionService.state,
                    screenSaverManager.showing,
                    appSettingsManager.useHardwareScreenOff,
                ) { taskState, saverShowing, hwFeatureEnabled ->
                    val taskActive = taskState == MaaExecutionState.STARTING
                            || taskState == MaaExecutionState.RUNNING
                            || taskState == MaaExecutionState.STOPPING
                    // 启用硬件熄屏功能后，前台时始终保持屏幕常亮、与任务状态解耦：
                    // 确保系统不会自动休眠/锁屏而中断正在运行的任务，硬件熄屏也无需再维护状态。
                    hwFeatureEnabled || taskActive || saverShowing
                }.collect { keepOn ->
                    if (keepOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun doObserveThemeMode() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appSettingsManager.themeMode.drop(1).collect { mode ->
                    val target = mode.toAppCompatNightMode()
                    if (delegate.localNightMode != target) {
                        delegate.localNightMode = target
                    }
                }
            }
        }
    }

    private fun AppSettingsManager.ThemeMode.toAppCompatNightMode(): Int = when (this) {
        AppSettingsManager.ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        AppSettingsManager.ThemeMode.WHITE -> AppCompatDelegate.MODE_NIGHT_NO
        AppSettingsManager.ThemeMode.DARK,
        AppSettingsManager.ThemeMode.PURE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }
}
