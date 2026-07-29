package com.aliothmoon.maameow.presentation.view.background

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import com.aliothmoon.maameow.presentation.benchmarkTestTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.DefaultDisplayConfig
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.domain.service.AppWatchdog
import com.aliothmoon.maameow.domain.service.ExternalNotificationService
import com.aliothmoon.maameow.domain.service.MaaCompositionService
import com.aliothmoon.maameow.domain.service.UnifiedStateDispatcher
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.overlay.screensaver.ScreenSaverOverlayManager
import com.aliothmoon.maameow.presentation.components.AdaptiveTaskPromptDialog
import com.aliothmoon.maameow.presentation.components.ExpressiveSwitch
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.components.ShizukuReadinessGate
import com.aliothmoon.maameow.presentation.view.panel.AutoBattlePanel
import com.aliothmoon.maameow.presentation.view.panel.LocalToolboxFileExporter
import com.aliothmoon.maameow.presentation.view.panel.LogPanel
import com.aliothmoon.maameow.presentation.view.panel.PanelDialogType
import com.aliothmoon.maameow.presentation.view.panel.PanelHeader
import com.aliothmoon.maameow.presentation.view.panel.PanelTab
import com.aliothmoon.maameow.presentation.view.panel.ToolboxPanel
import com.aliothmoon.maameow.presentation.view.panel.rememberSafToolboxFileExporter
import com.aliothmoon.maameow.presentation.viewmodel.BackgroundTaskViewModel
import com.aliothmoon.maameow.presentation.viewmodel.CopilotViewModel
import com.aliothmoon.maameow.presentation.viewmodel.ToolboxViewModel
import com.aliothmoon.maameow.schedule.service.ScheduledLaunchInbox
import com.aliothmoon.maameow.theme.MaaAnimations
import com.aliothmoon.maameow.utils.i18n.asString
import com.aliothmoon.maameow.utils.i18n.resolve
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun BackgroundTaskView(
    navController: NavController,
    viewModel: BackgroundTaskViewModel = koinViewModel(),
    copilotViewModel: CopilotViewModel = koinInject(),
    toolboxViewModel: ToolboxViewModel = koinInject(),
    compositionService: MaaCompositionService = koinInject(),
    dispatcher: UnifiedStateDispatcher = koinInject(),
    screenSaverManager: ScreenSaverOverlayManager = koinInject(),
    appWatchdog: AppWatchdog = koinInject(),
    appSettingsManager: AppSettingsManager = koinInject(),
    permissionManager: PermissionManager = koinInject(),
    scheduledLaunchInbox: ScheduledLaunchInbox = koinInject(),
    externalNotificationService: ExternalNotificationService = koinInject(),
) {

    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val maaState by compositionService.state.collectAsStateWithLifecycle()
    val runMode by appSettingsManager.runMode.collectAsStateWithLifecycle()
    val permissionState by permissionManager.state.collectAsStateWithLifecycle()
    val displayResolution by compositionService.displayResolution.collectAsStateWithLifecycle()
    val displayAspectRatio = if (displayResolution.width > 0 && displayResolution.height > 0) {
        displayResolution.width.toFloat() / displayResolution.height.toFloat()
    } else {
        DefaultDisplayConfig.ASPECT_RATIO
    }
    val isChainLoaded by viewModel.chainState.isLoaded.collectAsStateWithLifecycle()
    var hasInitialized by rememberSaveable { mutableStateOf(false) }
    if (isChainLoaded) {
        hasInitialized = true
    }
    val isInitialized = hasInitialized

    var showCloseConfirm by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }

    val copilotDialog by copilotViewModel.dialog.collectAsStateWithLifecycle()
    val toolboxDialog by toolboxViewModel.dialog.collectAsStateWithLifecycle()
    val nodes by viewModel.chainState.chain.collectAsStateWithLifecycle()
    val profiles by viewModel.chainState.profiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.chainState.activeProfileId.collectAsStateWithLifecycle()
    val selectedNode = nodes.find { it.id == state.selectedNodeId }
    val canShowTaskActions = PanelTab.canShowTaskActions(state.current)

    val pagerState = rememberPagerState(
        initialPage = state.current.ordinal, pageCount = { PanelTab.entries.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val newTab = PanelTab.entries[page]
            if (newTab != state.current) {
                viewModel.onTabChange(newTab)
            }
        }
    }

    LaunchedEffect(state.current) {
        if (pagerState.currentPage != state.current.ordinal) {
            pagerState.animateScrollToPage(
                state.current.ordinal, animationSpec = tween(
                    easing = MaaAnimations.springEasing, durationMillis = 250
                )
            )
        }
    }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val serviceDiedMessage = stringResource(R.string.bg_toast_service_died)
    val appDiedMessage = stringResource(R.string.bg_toast_app_died)
    val foregroundBlocked = runMode == RunMode.FOREGROUND
    val backendBlocked =
        !permissionState.isStartupBackendAvailable(permissionState.startupBackend)
    val startBlocked = foregroundBlocked || backendBlocked
    val switchBackgroundModeMessage =
        stringResource(R.string.navigation_toast_switch_background_mode)
    val backendUnavailableMessage = stringResource(
        R.string.home_toast_backend_unavailable,
        permissionState.startupBackend.display,
    )

    ShizukuReadinessGate()


    val pendingExecution by viewModel.coordinator.pendingExecution.collectAsStateWithLifecycle()
    val incomingScheduledExecution by scheduledLaunchInbox.pending.collectAsStateWithLifecycle()

    LaunchedEffect(incomingScheduledExecution?.requestId) {
        incomingScheduledExecution?.let { request ->
            scheduledLaunchInbox.consume(request.requestId)?.let(viewModel::onScheduledLaunch)
        }
    }

    LaunchedEffect(pendingExecution?.requestId) {
        pendingExecution?.let { request ->
            viewModel.onScheduledExecutionPageReady(request.requestId)
        }
    }

    LaunchedEffect(Unit) {
        externalNotificationService.feedbackMessages.collect { message ->
            Toast.makeText(context, message.resolve(context), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            if (effect is com.aliothmoon.maameow.presentation.state.UiEffect.Toast) {
                Toast.makeText(context, effect.message.resolve(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        dispatcher.serviceDiedEvent.collect {
            Toast.makeText(
                context, serviceDiedMessage, Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        appWatchdog.appDiedEvent.collect {
            Toast.makeText(
                context, appDiedMessage, Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.screenshotMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val shouldHideMoreActions =
        !canShowTaskActions || showCloseConfirm || state.isFullscreenMonitor || state.dialog != null
    LaunchedEffect(shouldHideMoreActions) {
        if (shouldHideMoreActions) showMoreActions = false
    }


    var isSurfaceAvailable by remember { mutableStateOf(false) }
    var lastSentSurface by remember { mutableStateOf<Surface?>(null) }
    val currentResolution by rememberUpdatedState(displayResolution)

    val previewContent = remember {
        movableContentOf {
            val innerScope = rememberCoroutineScope()
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                val resolution = currentResolution
                val surfaceAspectRatio = if (resolution.width > 0 && resolution.height > 0) {
                    resolution.width.toFloat() / resolution.height.toFloat()
                } else {
                    DefaultDisplayConfig.ASPECT_RATIO
                }
                Box(modifier = Modifier.aspectRatio(surfaceAspectRatio)) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).apply {
                                holder.setFormat(PixelFormat.RGBA_8888)
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        isSurfaceAvailable = true
                                        innerScope.launch {
                                            delay(50)
                                            val res = currentResolution
                                            holder.setFixedSize(res.width, res.height)
                                        }
                                    }

                                    override fun surfaceChanged(
                                        holder: SurfaceHolder, format: Int, width: Int, height: Int
                                    ) {
                                        Timber.d("Surface size changed to $width x $height")
                                        val res = currentResolution
                                        if (width == res.width && height == res.height) {
                                            if (lastSentSurface != holder.surface) {
                                                lastSentSurface = holder.surface
                                                viewModel.onSurfaceAvailable(holder.surface)
                                            }
                                        }
                                    }

                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        isSurfaceAvailable = false
                                        lastSentSurface = null
                                        viewModel.onSurfaceDestroyed()
                                    }
                                })
                            }
                        }, modifier = Modifier.fillMaxSize()
                    )
                    TouchPreviewLayer(viewModel, displayResolution)
                }
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize().benchmarkTestTag("background_page")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 2.dp)
        ) {
            // --- 预览图区域：实时加载 ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(displayAspectRatio)
            ) {
                if (!state.isFullscreenMonitor) {
                    VirtualDisplayPreview(
                        modifier = Modifier.fillMaxSize(),
                        aspectRatio = displayAspectRatio,
                        isRunning = maaState == MaaExecutionState.RUNNING,
                        isSurfaceAvailable = isSurfaceAvailable,
                        onClick = { viewModel.onToggleFullscreenMonitor() }) {
                        previewContent()
                    }
                    if (isInitialized && canShowTaskActions) {
                        val isStopping = maaState == MaaExecutionState.RUNNING ||
                            maaState == MaaExecutionState.STOPPING
                        val isTransitioning = maaState == MaaExecutionState.STARTING ||
                            maaState == MaaExecutionState.STOPPING
                        val actionContainerColor = when {
                            isStopping -> MaterialTheme.colorScheme.error
                            startBlocked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val actionContentColor = when {
                            isStopping -> MaterialTheme.colorScheme.onError
                            startBlocked -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onPrimary
                        }

                        Surface(
                            onClick = {
                                focusManager.clearFocus()
                                if (maaState == MaaExecutionState.RUNNING) {
                                    when (state.current) {
                                        PanelTab.TASKS -> viewModel.onStopTasks()
                                        PanelTab.AUTO_BATTLE -> copilotViewModel.onStop()
                                        PanelTab.TOOLS -> toolboxViewModel.onStop()
                                        else -> Unit
                                    }
                                } else {
                                    when {
                                        foregroundBlocked -> Toast.makeText(
                                            context,
                                            switchBackgroundModeMessage,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        backendBlocked -> Toast.makeText(
                                            context,
                                            backendUnavailableMessage,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        else -> when (state.current) {
                                            PanelTab.TASKS -> viewModel.onStartTasks()
                                            PanelTab.AUTO_BATTLE -> copilotViewModel.onStart()
                                            PanelTab.TOOLS -> toolboxViewModel.onStart()
                                            else -> Unit
                                        }
                                    }
                                }
                            },
                            enabled = !isTransitioning,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .size(48.dp),
                            shape = CircleShape,
                            color = actionContainerColor,
                            contentColor = actionContentColor,
                            shadowElevation = 3.dp,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isTransitioning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = actionContentColor,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isStopping) {
                                            Icons.Filled.Stop
                                        } else {
                                            Icons.Filled.PlayArrow
                                        },
                                        contentDescription = stringResource(
                                            if (isStopping) {
                                                R.string.task_btn_stop
                                            } else {
                                                R.string.task_btn_start
                                            },
                                        ),
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = { showMoreActions = !showMoreActions },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .size(48.dp),
                            shape = CircleShape,
                            color = if (showMoreActions) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (showMoreActions) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            shadowElevation = 3.dp,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(
                                        R.string.task_more_actions_cd,
                                    ),
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 业务内容区域：阶梯加载 ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                PanelHeader(
                    selectedTab = state.current,
                    onTabSelected = { tab -> viewModel.onTabChange(tab) },
                    tabs = PanelTab.entries.filterNot { it == PanelTab.LOG },
                    onLogClick = {
                        viewModel.onTabChange(
                            if (state.current == PanelTab.LOG) PanelTab.TASKS else PanelTab.LOG
                        )
                    },
                    showActions = false
                )

                BackHandler(enabled = state.current == PanelTab.LOG) {
                    viewModel.onTabChange(PanelTab.TASKS)
                }

                if (isInitialized) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            userScrollEnabled = false,
                            beyondViewportPageCount = 0
                        ) { page ->
                            when (page) {
                                0 -> TaskProfileSelectorPanel(
                                    profiles = profiles,
                                    activeProfileId = activeProfileId,
                                    onSwitchProfile = viewModel::onSwitchProfile,
                                    onEditProfile = { profileId ->
                                        viewModel.onSwitchProfile(profileId)
                                        navController.navigate(Routes.TASK_PROFILE_EDITOR)
                                    },
                                    onCreateProfile = viewModel::onCreateProfile,
                                    onReorderProfile = viewModel::onReorderProfile,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                1 -> AutoBattlePanel(modifier = Modifier.fillMaxSize())
                                2 -> CompositionLocalProvider(
                                    LocalToolboxFileExporter provides rememberSafToolboxFileExporter()
                                ) {
                                    ToolboxPanel(modifier = Modifier.fillMaxSize())
                                }

                                3 -> {
                                    val runtimeLogs by viewModel.logs.collectAsStateWithLifecycle()
                                    LogPanel(
                                        logs = runtimeLogs,
                                        onClearLogs = { viewModel.onClearLogs() },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 初始化中的骨架占位
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp), strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        BackHandler(enabled = showMoreActions) {
            showMoreActions = false
        }

        if (showMoreActions) {
            val isGameMuted by viewModel.isGameMuted.collectAsStateWithLifecycle()
            BackgroundMoreActionsOverlay(
                onDismissRequest = { showMoreActions = false },
                isGameMuted = isGameMuted,
                onToggleGameSound = viewModel::onToggleGameSound,
                onScreenOff = viewModel::onScreenOff,
                onShowScreenSaver = { screenSaverManager.show(context as? Activity) },
                onCaptureScreenshot = viewModel::onCaptureDebugScreenshot,
                onCloseApp = {
                    if (maaState == MaaExecutionState.RUNNING) {
                        showCloseConfirm = true
                    } else {
                        coroutineScope.launch { compositionService.stopVirtualDisplay() }
                    }
                },
            )
        }

        // 全屏预览
        if (state.isFullscreenMonitor) {
            val activity = context as? Activity

            DisposableEffect(Unit) {
                val window = activity?.window
                val controller = window?.let {
                    WindowCompat.getInsetsController(it, it.decorView)
                }
                controller?.hide(WindowInsetsCompat.Type.systemBars())
                controller?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                onDispose {
                    controller?.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            DisposableEffect(Unit) {
                val originalOrientation = activity?.requestedOrientation
                onDispose {
                    if (originalOrientation != null) {
                        activity.requestedOrientation = originalOrientation
                    }
                }
            }

            LaunchedEffect(Unit) {
                val current = activity?.resources?.configuration?.orientation
                if (current != Configuration.ORIENTATION_LANDSCAPE) {
                    activity?.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }

            BackHandler { viewModel.onToggleFullscreenMonitor() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                viewToVirtualDisplay(
                                    viewX = change.position.x,
                                    viewY = change.position.y,
                                    viewWidth = size.width,
                                    viewHeight = size.height,
                                    bufferWidth = displayResolution.width,
                                    bufferHeight = displayResolution.height
                                ) { vx, vy ->
                                    when (event.type) {
                                        PointerEventType.Press -> viewModel.onTouchDown(vx, vy)
                                        PointerEventType.Move -> {
                                            if (change.pressed) {
                                                viewModel.onTouchMove(vx, vy)
                                            }
                                        }

                                        PointerEventType.Release -> viewModel.onTouchUp(vx, vy)
                                    }
                                }
                                change.consume()
                            }
                        }
                    }, contentAlignment = Alignment.Center
            ) {
                previewContent()

                IconButton(
                    onClick = { viewModel.onToggleFullscreenMonitor() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.task_close_preview_cd),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        val activeDialog = state.dialog ?: copilotDialog ?: toolboxDialog
        val activeDialogCallbacks = when {
            state.dialog != null -> viewModel::onDialogDismiss to viewModel::onDialogConfirm
            copilotDialog != null -> copilotViewModel::onDialogDismiss to copilotViewModel::onDialogConfirm
            toolboxDialog != null -> toolboxViewModel::onDialogDismiss to toolboxViewModel::onDialogConfirm
            else -> null
        }
        activeDialog?.let { dialog ->
            val (onDismiss, onConfirm) = activeDialogCallbacks!!
            val confirmColor = when (dialog.type) {
                PanelDialogType.SUCCESS -> MaterialTheme.colorScheme.primary
                PanelDialogType.WARNING -> MaterialTheme.colorScheme.tertiary
                PanelDialogType.ERROR -> MaterialTheme.colorScheme.error
            }
            val dialogTitle = dialog.title.asString()
            val dialogMessage = dialog.message.asString()
            val dialogConfirmText = dialog.confirmText.asString()
            val dialogDismissText = dialog.dismissText.asString()
            AdaptiveTaskPromptDialog(
                visible = true,
                title = dialogTitle,
                message = AnnotatedString(dialogMessage),
                onDismissRequest = onDismiss,
                onConfirm = onConfirm,
                confirmText = dialogConfirmText.ifBlank {
                    stringResource(R.string.common_confirm)
                },
                dismissText = dialogDismissText.ifBlank {
                    stringResource(R.string.common_close)
                },
                icon = when (dialog.type) {
                    PanelDialogType.SUCCESS -> Icons.Filled.CheckCircle
                    else -> Icons.Filled.Warning
                },
                iconTint = confirmColor,
                confirmColor = confirmColor,
            )
        }

        if (showCloseConfirm) {
            AdaptiveTaskPromptDialog(
                visible = true,
                title = stringResource(R.string.dialog_close_app_title),
                message = AnnotatedString(stringResource(R.string.dialog_close_app_message)),
                onDismissRequest = { showCloseConfirm = false },
                onConfirm = {
                    showCloseConfirm = false
                    coroutineScope.launch { compositionService.stopVirtualDisplay() }
                },
                confirmText = stringResource(R.string.dialog_close_app_confirm),
                dismissText = stringResource(R.string.common_cancel),
                icon = Icons.Filled.Warning,
                iconTint = MaterialTheme.colorScheme.error,
                confirmColor = MaterialTheme.colorScheme.error,
            )
        }

    }
}

@Composable
private fun TouchPreviewLayer(
    viewModel: BackgroundTaskViewModel,
    displayResolution: DefaultDisplayConfig.Resolution,
) {
    val markers by viewModel.markers.collectAsStateWithLifecycle()
    if (markers.isNotEmpty()) {
        TouchPreviewOverlay(
            markers = markers,
            displayResolution = displayResolution,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private inline fun viewToVirtualDisplay(
    viewX: Float,
    viewY: Float,
    viewWidth: Int,
    viewHeight: Int,
    bufferWidth: Int,
    bufferHeight: Int,
    block: (vx: Int, vy: Int) -> Unit,
) {
    val bufferW = bufferWidth.toFloat()
    val bufferH = bufferHeight.toFloat()
    val scale = minOf(viewWidth / bufferW, viewHeight / bufferH)
    val offsetX = (viewWidth - bufferW * scale) / 2f
    val offsetY = (viewHeight - bufferH * scale) / 2f
    val vx = ((viewX - offsetX) / scale).toInt()
    val vy = ((viewY - offsetY) / scale).toInt()
    if (vx < 0 || vx >= bufferW.toInt() || vy < 0 || vy >= bufferH.toInt()) return
    block(vx, vy)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundMoreActionsOverlay(
    onDismissRequest: () -> Unit,
    isGameMuted: Boolean,
    onToggleGameSound: () -> Unit,
    onScreenOff: () -> Unit,
    onShowScreenSaver: () -> Unit,
    onCaptureScreenshot: () -> Unit,
    onCloseApp: () -> Unit,
    appSettingsManager: AppSettingsManager = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val muteOnGameLaunch by appSettingsManager.muteOnGameLaunch.collectAsStateWithLifecycle()
    val closeAppOnTaskEnd by appSettingsManager.closeAppOnTaskEnd.collectAsStateWithLifecycle()
    val useHardwareScreenOff by appSettingsManager.useHardwareScreenOff.collectAsStateWithLifecycle()
    val showTouchPreview by appSettingsManager.showTouchPreview.collectAsStateWithLifecycle()
    val debugMode by appSettingsManager.debugMode.collectAsStateWithLifecycle()
    var showHardwareScreenOffConfirm by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.task_more_actions_cd),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.bg_actions_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SheetActionButton(
                    icon = Icons.Filled.PowerSettingsNew,
                    label = stringResource(R.string.bg_action_screen_off),
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismissRequest()
                            if (useHardwareScreenOff) onScreenOff() else onShowScreenSaver()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = sheetActionGridShape(
                        rowIndex = 0,
                        rowCount = 2,
                        columnIndex = 0,
                        columnCount = 2,
                    ),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
                SheetActionButton(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = stringResource(R.string.bg_action_close_game),
                    onClick = onCloseApp,
                    modifier = Modifier.weight(1f),
                    shape = sheetActionGridShape(
                        rowIndex = 0,
                        rowCount = 2,
                        columnIndex = 1,
                        columnCount = 2,
                    ),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val lastIndex = if (debugMode) 1 else 0
                SheetActionButton(
                    icon = if (isGameMuted) {
                        Icons.AutoMirrored.Filled.VolumeOff
                    } else {
                        Icons.AutoMirrored.Filled.VolumeUp
                    },
                    label = if (isGameMuted) {
                        stringResource(R.string.bg_action_game_muted)
                    } else {
                        stringResource(R.string.bg_action_mute_game)
                    },
                    onClick = onToggleGameSound,
                    modifier = Modifier.weight(1f),
                    shape = sheetActionGridShape(
                        rowIndex = 1,
                        rowCount = 2,
                        columnIndex = 0,
                        columnCount = lastIndex + 1,
                    ),
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                )
                if (debugMode) {
                    SheetActionButton(
                        icon = Icons.Filled.Screenshot,
                        label = stringResource(R.string.bg_action_screenshot),
                        onClick = onCaptureScreenshot,
                        modifier = Modifier.weight(1f),
                        shape = sheetActionGridShape(
                            rowIndex = 1,
                            rowCount = 2,
                            columnIndex = 1,
                            columnCount = lastIndex + 1,
                        ),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Text(
                text = stringResource(R.string.bg_auto_settings_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )

            SegmentedSettingsGroup {
                item {
                    SheetSwitchRow(
                        icon = Icons.Filled.NotificationsPaused,
                        label = stringResource(R.string.bg_auto_mute_on_launch),
                        checked = muteOnGameLaunch,
                        onCheckedChange = {
                            coroutineScope.launch { appSettingsManager.setMuteOnGameLaunch(it) }
                        },
                    )
                }
                item {
                    SheetSwitchRow(
                        icon = Icons.Filled.Cancel,
                        label = stringResource(R.string.bg_auto_close_on_end),
                        checked = closeAppOnTaskEnd,
                        onCheckedChange = {
                            coroutineScope.launch { appSettingsManager.setCloseAppOnTaskEnd(it) }
                        },
                    )
                }
                item {
                    SheetSwitchRow(
                        icon = Icons.Filled.StayCurrentPortrait,
                        label = stringResource(R.string.bg_auto_hardware_screen_off),
                        checked = useHardwareScreenOff,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showHardwareScreenOffConfirm = true
                            } else {
                                coroutineScope.launch {
                                    appSettingsManager.setUseHardwareScreenOff(false)
                                }
                            }
                        },
                    )
                }
                item {
                    SheetSwitchRow(
                        icon = Icons.Filled.TouchApp,
                        label = stringResource(R.string.bg_auto_show_touch_preview),
                        checked = showTouchPreview,
                        onCheckedChange = {
                            coroutineScope.launch { appSettingsManager.setShowTouchPreview(it) }
                        },
                    )
                }
            }
        }
    }

    if (showHardwareScreenOffConfirm) {
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.dialog_hardware_screen_off_title),
            message = AnnotatedString(stringResource(R.string.dialog_hardware_screen_off_message)),
            onDismissRequest = { showHardwareScreenOffConfirm = false },
            onConfirm = {
                showHardwareScreenOffConfirm = false
                coroutineScope.launch { appSettingsManager.setUseHardwareScreenOff(true) }
            },
            confirmText = stringResource(R.string.common_confirm),
            dismissText = stringResource(R.string.common_cancel),
            icon = Icons.Filled.PowerSettingsNew,
            iconTint = MaterialTheme.colorScheme.primary,
            confirmColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SheetActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SheetSwitchRow(
    icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    SettingRow(
        title = label,
        icon = icon,
        trailing = {
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = null,
            )
        },
        onClick = { onCheckedChange(!checked) },
    )
}

private fun sheetActionGridShape(
    rowIndex: Int,
    rowCount: Int,
    columnIndex: Int,
    columnCount: Int,
) = RoundedCornerShape(
    topStart = if (rowIndex == 0 && columnIndex == 0) 16.dp else 5.dp,
    topEnd = if (rowIndex == 0 && columnIndex == columnCount - 1) 16.dp else 5.dp,
    bottomStart = if (rowIndex == rowCount - 1 && columnIndex == 0) 16.dp else 5.dp,
    bottomEnd = if (
        rowIndex == rowCount - 1 && columnIndex == columnCount - 1
    ) 16.dp else 5.dp,
)
