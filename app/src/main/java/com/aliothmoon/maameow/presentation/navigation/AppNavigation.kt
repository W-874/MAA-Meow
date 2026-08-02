package com.aliothmoon.maameow.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aliothmoon.maameow.announcement.AnnouncementConfig
import com.aliothmoon.maameow.BuildConfig
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.domain.service.ResourceInitService
import com.aliothmoon.maameow.domain.state.ResourceInitState
import com.aliothmoon.maameow.presentation.LocalToaster
import com.aliothmoon.maameow.presentation.enableBenchmarkTestTags
import com.aliothmoon.maameow.presentation.components.AnnouncementDialog
import com.aliothmoon.maameow.presentation.components.ResourceLoadingOverlay
import com.aliothmoon.maameow.presentation.components.clearFocusOnBlankTap
import com.aliothmoon.maameow.presentation.state.UiEffect
import com.aliothmoon.maameow.presentation.state.BackgroundChromeState
import com.aliothmoon.maameow.presentation.view.notification.NotificationSettingsView
import com.aliothmoon.maameow.presentation.view.background.TaskProfileEditorView
import com.aliothmoon.maameow.presentation.view.settings.AchievementDebugView
import com.aliothmoon.maameow.presentation.view.settings.AchievementView
import com.aliothmoon.maameow.presentation.view.settings.AboutView
import com.aliothmoon.maameow.presentation.view.settings.ErrorLogView
import com.aliothmoon.maameow.presentation.view.settings.LogHistoryView
import com.aliothmoon.maameow.presentation.view.settings.TaskOverrideEditorView
import com.aliothmoon.maameow.presentation.viewmodel.AppEventsViewModel
import com.aliothmoon.maameow.schedule.model.CountdownState
import com.aliothmoon.maameow.schedule.service.ScheduledLaunchInbox
import com.aliothmoon.maameow.schedule.service.ScheduledLaunchUiState
import com.aliothmoon.maameow.schedule.ui.CountdownDialog
import com.aliothmoon.maameow.schedule.ui.ScheduleEditView
import com.aliothmoon.maameow.schedule.ui.ScheduleTriggerLogView
import com.aliothmoon.maameow.theme.MaaAnimations
import com.aliothmoon.maameow.utils.i18n.resolve
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun AppNavigation(
    appSettings: AppSettingsManager = koinInject(),
    appEventsViewModel: AppEventsViewModel = koinViewModel(),
    backgroundChromeState: BackgroundChromeState = koinInject(),
    scheduledLaunchInbox: ScheduledLaunchInbox = koinInject(),
    scheduledLaunchUiState: ScheduledLaunchUiState = koinInject(),
    resourceInitService: ResourceInitService = koinInject(),
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val toaster = rememberToasterState()
    val isFullscreen by backgroundChromeState.fullscreen.collectAsStateWithLifecycle()
    var forceShowAnnouncement by remember { mutableStateOf(false) }
    var announcementDismissedOnce by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val runMode by appSettings.runMode.collectAsStateWithLifecycle()
    val announcementReadVersion by appSettings.announcementReadVersion.collectAsStateWithLifecycle()
    val language by appSettings.language.collectAsStateWithLifecycle()
    val scheduledCountdownState by scheduledLaunchUiState.countdown.collectAsStateWithLifecycle()
    val resourcesReady by remember(resourceInitService) {
        resourceInitService.state
            .map { it is ResourceInitState.Ready }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(scheduledLaunchUiState) {
        scheduledLaunchUiState.feedbackMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(appEventsViewModel) {
        appEventsViewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.Toast -> toaster.show(
                    message = effect.message.resolve(context),
                    type = ToastType.Success,
                )
            }
        }
    }
    LaunchedEffect(scheduledLaunchInbox) {
        scheduledLaunchInbox.pending.collect { request ->
            if (request != null && navController.currentDestination?.route != Routes.HOME) {
                navController.popBackStack(Routes.HOME, false)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .enableBenchmarkTestTags()
            .clearFocusOnBlankTap()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        // NavHost 以 MainScreen 为根页面，子页面压入其上以保证预测返回能恢复主界面。
        // 在此统一下发 LocalToaster，使所有子页面都能弹出顶部提示。
        CompositionLocalProvider(LocalToaster provides toaster) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                enterTransition = { MaaAnimations.sharedAxisForwardEnter },
                exitTransition = { MaaAnimations.sharedAxisForwardExit },
                popEnterTransition = { MaaAnimations.sharedAxisPopEnter },
                popExitTransition = { MaaAnimations.sharedAxisPopExit },
            ) {
                composable(Routes.HOME) {
                    MainScreen(
                        navController = navController,
                        onViewAnnouncement = { forceShowAnnouncement = true },
                        fullscreen = isFullscreen,
                    )
                }

                composable(Routes.NOTIFICATION) {
                    NotificationSettingsView(navController = navController)
                }
                composable(Routes.ACHIEVEMENT) {
                    AchievementView(navController = navController)
                }
                composable(Routes.ACHIEVEMENT_DEBUG) {
                    AchievementDebugView(navController = navController)
                }
                composable(Routes.LOG_HISTORY) {
                    LogHistoryView(navController = navController)
                }
                composable(Routes.ERROR_LOG) {
                    ErrorLogView(navController = navController)
                }
                composable(Routes.TASK_PROFILE_EDITOR) {
                    TaskProfileEditorView(
                        navController = navController,
                        viewModel = koinViewModel(),
                    )
                }
                composable(Routes.SCHEDULE_EDIT) { backStackEntry ->
                    val strategyId = backStackEntry.arguments?.getString("strategyId")
                        .let { if (it == "new") null else it }
                    ScheduleEditView(navController = navController, strategyId = strategyId)
                }
                composable(Routes.SCHEDULE_TRIGGER_LOG) {
                    ScheduleTriggerLogView(navController = navController)
                }
                composable(Routes.TASK_OVERRIDE_EDITOR) {
                    TaskOverrideEditorView(navController = navController)
                }
                composable(Routes.ABOUT) {
                    AboutView(navController = navController)
                }
            }
        }
        ResourceLoadingOverlay()
        // 顶部轻提示（sonner）：替代旧的 Material3 Snackbar，按类型上色（成功=绿、错误=红）
        Toaster(
            state = toaster,
            alignment = Alignment.TopCenter,
            richColors = true,
            showCloseButton = true,
            darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f,
            containerPadding = PaddingValues(top = 8.dp),
            modifier = Modifier.statusBarsPadding(),
        )
        // 全局定时任务倒计时弹窗（前台所有控制模式均不弹出对话框，静默处理）
        val countdown = scheduledCountdownState
        val hideCountdownDialog = runMode == RunMode.FOREGROUND
        if (countdown is CountdownState.Counting && !hideCountdownDialog) {
            CountdownDialog(
                state = countdown,
                onCancel = scheduledLaunchUiState::cancel,
                onStartNow = scheduledLaunchUiState::startNow,
            )
        }
        // 长期公告弹窗：每次公告版本变更后首次启动自动弹出，或从设置中手动打开
        val needsToShow = !BuildConfig.BENCHMARK_BUILD &&
            !BuildConfig.BUILD_TYPE.contains("benchmark", ignoreCase = true) &&
            !BuildConfig.BUILD_TYPE.contains("nonMinified", ignoreCase = true) &&
            resourcesReady &&
            announcementReadVersion != AnnouncementConfig.CURRENT_VERSION
        val showAnnouncement = forceShowAnnouncement || (needsToShow && !announcementDismissedOnce)
        val announcementMarkdown = remember(showAnnouncement, language) {
            if (showAnnouncement) {
                AnnouncementConfig.loadContent(context, language)
            } else {
                null
            }
        }
        if (announcementMarkdown != null) {
            AnnouncementDialog(
                imageAssetPath = remember(language) { AnnouncementConfig.imageAssetPath(language) },
                markdown = announcementMarkdown,
                onDismiss = { dontShowAgain ->
                    forceShowAnnouncement = false
                    if (dontShowAgain) {
                        coroutineScope.launch {
                            appSettings.setAnnouncementReadVersion(AnnouncementConfig.CURRENT_VERSION)
                        }
                    } else {
                        announcementDismissedOnce = true
                    }
                },
            )
        }
    }
}
