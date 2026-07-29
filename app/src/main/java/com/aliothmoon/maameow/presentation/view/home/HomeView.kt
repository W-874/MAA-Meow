package com.aliothmoon.maameow.presentation.view.home

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.datasource.ResourceDownloader
import com.aliothmoon.maameow.data.model.update.UpdateCheckResult
import com.aliothmoon.maameow.data.model.update.UpdateInfo
import com.aliothmoon.maameow.data.model.update.UpdateProcessState
import com.aliothmoon.maameow.data.permission.PermissionState
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.OverlayControlMode
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.domain.state.ResourceInitState
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.presentation.components.AdaptiveTaskPromptDialog
import com.aliothmoon.maameow.presentation.components.ShizukuReadinessGate
import com.aliothmoon.maameow.presentation.components.ChangelogDialog
import com.aliothmoon.maameow.presentation.components.ResourceInitDialog
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.components.SettingActionButton
import com.aliothmoon.maameow.presentation.components.SettingDropdown
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.UpdateCard
import com.aliothmoon.maameow.presentation.benchmarkTestTag
import com.aliothmoon.maameow.presentation.navigation.LocalMainBottomBarPadding
import com.aliothmoon.maameow.presentation.state.StatusColorType
import com.aliothmoon.maameow.presentation.state.UiEffect
import com.aliothmoon.maameow.presentation.viewmodel.HomeViewModel
import com.aliothmoon.maameow.presentation.viewmodel.UpdateViewModel
import com.aliothmoon.maameow.utils.Misc
import com.aliothmoon.maameow.utils.i18n.UiText
import com.aliothmoon.maameow.utils.i18n.asString
import com.aliothmoon.maameow.utils.i18n.overlayControlModeDisplayName
import com.aliothmoon.maameow.utils.i18n.resolve
import com.aliothmoon.maameow.utils.i18n.runModeDisplayName
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import timber.log.Timber


@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun HomeView(
    navController: NavController,
    onViewAnnouncement: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
    updateViewModel: UpdateViewModel = koinViewModel(),
    permissionManager: PermissionManager = koinInject(),
    appSettingsManager: AppSettingsManager = koinInject(),
) {
    val mainBottomBarPadding = LocalMainBottomBarPadding.current

    val context = LocalContext.current
    val (width, height) = remember(context) { Misc.getScreenSize(context) }

    // 启动时检查资源初始化
    LaunchedEffect(Unit) {
        viewModel.checkAndInitResource()
    }

    // 自动下载失败时弹 Toast
    LaunchedEffect(Unit) {
        updateViewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.Toast -> Toast.makeText(
                    context,
                    effect.message.resolve(context),
                    if (effect.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    // 资源初始化完成后刷新版本号
    LaunchedEffect(viewModel) {
        viewModel.resourceInitState.collect { state ->
            if (state is ResourceInitState.Ready) {
                updateViewModel.refreshResourceVersion()
                updateViewModel.checkPendingChangelog()
                updateViewModel.checkUpdatesOnStartup()
            }
        }
    }

    ResourceInitDialogHost(viewModel)

    // 更新公告弹窗
    val changelogDialog by updateViewModel.changelogDialog.collectAsStateWithLifecycle()
    changelogDialog?.let {
        ChangelogDialog(
            content = it,
            onDismiss = { updateViewModel.dismissChangelog() }
        )
    }

    RunModeUnsupportedDialogHost(viewModel)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.home_app_title),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .benchmarkTestTag("home_list"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() +
                        mainBottomBarPadding + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "screen_info", contentType = "home_screen_info") {
                    val serviceUiState by viewModel.serviceUiState.collectAsStateWithLifecycle()
                    ScreenInfoCard(
                        screenWidth = width,
                        screenHeight = height,
                        serviceStatusColor = serviceUiState.serviceStatusColor,
                        serviceStatusText = serviceUiState.serviceStatusText,
                        serviceStatusLoading = serviceUiState.serviceStatusLoading
                    )
                }

                item(key = "home_updates", contentType = "home_updates") {
                    HomeOperationStatusCardHost(viewModel, updateViewModel)
                }

                item(key = "runtime_info", contentType = "home_runtime_info") {
                    val serviceUiState by viewModel.serviceUiState.collectAsStateWithLifecycle()
                    val interactionUiState by viewModel.interactionUiState.collectAsStateWithLifecycle()
                    val permissionState by permissionManager.state.collectAsStateWithLifecycle()
                    val shizukuShortcutEnabled by appSettingsManager.shizukuShortcutEnabled.collectAsStateWithLifecycle()
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RuntimeInfoSection(
                            runMode = interactionUiState.runMode,
                            onRunModeSelected = {
                                viewModel.onRunModeChange(it == RunMode.BACKGROUND)
                            },
                            changeEnabled = viewModel.checkRunModeChangeEnabled(),
                            permissionState = permissionState,
                            isGranting = interactionUiState.isGranting,
                            onRequestShizukuAccess = { viewModel.onRequestShizukuAccess() },
                            remoteServiceActive = serviceUiState.remoteServiceActive,
                            isLoading = serviceUiState.isLoading,
                            shizukuShortcutEnabled = shizukuShortcutEnabled,
                            onOpenShizuku = { viewModel.onOpenShizuku() },
                            onCloseRemoteService = { viewModel.onToggleRemoteService() },
                        )
                        if (interactionUiState.runMode == RunMode.FOREGROUND) {
                            ForegroundModeSectionHost(viewModel)
                        }
                    }
                }

                item(key = "learn_more", contentType = "home_learn_more") {
                    SegmentedSettingsGroup {
                        item {
                            SettingRow(
                                title = stringResource(R.string.home_announcement_title),
                                description = stringResource(R.string.home_announcement_desc),
                                icon = null,
                                onClick = onViewAnnouncement,
                            )
                        }
                    }
                }

            }
        }

        ShizukuReadinessGate()
    }
}

@Composable
private fun RunModeUnsupportedDialogHost(viewModel: HomeViewModel) {
    val state by viewModel.interactionUiState.collectAsStateWithLifecycle()
    if (!state.showRunModeUnsupportedDialog) return
    AdaptiveTaskPromptDialog(
        visible = true,
        title = stringResource(R.string.dialog_run_mode_unsupported_title),
        message = state.runModeUnsupportedMessage.asString(),
        confirmText = stringResource(R.string.common_i_got_it),
        dismissText = null,
        onConfirm = viewModel::onDismissRunModeUnsupportedDialog,
        onDismissRequest = viewModel::onDismissRunModeUnsupportedDialog,
    )
}

@Composable
private fun ForegroundModeSectionHost(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val serviceState by viewModel.serviceUiState.collectAsStateWithLifecycle()
    val interactionState by viewModel.interactionUiState.collectAsStateWithLifecycle()
    if (interactionState.runMode != RunMode.FOREGROUND) return
    ForegroundModeSection(
        overlayControlMode = interactionState.overlayControlMode,
        isShowControlOverlay = interactionState.isShowControlOverlay,
        isLoading = serviceState.isLoading,
        onChangeTo16x9Resolution = { viewModel.onChangeTo16x9Resolution(context) },
        onResetResolution = viewModel::onResetResolution,
        onControlOverlayModeChanged = viewModel::onControlOverlayModeChanged,
        onToggleOverlay = {
            if (interactionState.isShowControlOverlay) {
                Timber.d("关闭悬浮窗")
                viewModel.onStopControlOverlay()
            } else {
                Timber.d("开启悬浮窗模式")
                viewModel.onStartControlOverlay()
            }
        },
    )
}

@Composable
private fun ResourceInitDialogHost(viewModel: HomeViewModel) {
    val state by viewModel.resourceInitState.collectAsStateWithLifecycle()
    ResourceInitDialog(
        state = state,
        onRetry = viewModel::onTryResourceInit,
    )
}

@Composable
private fun HomeOperationStatusCardHost(
    viewModel: HomeViewModel,
    updateViewModel: UpdateViewModel,
) {
    val resourceInitState by viewModel.resourceInitState.collectAsStateWithLifecycle()
    val startupUpdate by updateViewModel.startupUpdateDialog.collectAsStateWithLifecycle()
    val appCheckResult by updateViewModel.appCheckResult.collectAsStateWithLifecycle()
    val resourceCheckResult by updateViewModel.resourceCheckResult.collectAsStateWithLifecycle()
    val appUpdateState by updateViewModel.appUpdateState.collectAsStateWithLifecycle()
    val resourceUpdateState by updateViewModel.resourceUpdateState.collectAsStateWithLifecycle()

    val extracting = resourceInitState as? ResourceInitState.Extracting
    val checkedAppUpdate = (appCheckResult as? UpdateCheckResult.Available)?.info
    val checkedResourceUpdate = (resourceCheckResult as? UpdateCheckResult.Available)?.info
    val appUpdate = startupUpdate?.appUpdate ?: checkedAppUpdate
    val resourceUpdate = startupUpdate?.resourceUpdate ?: checkedResourceUpdate

    val statusKind = when {
        extracting != null -> HomeOperationStatusKind.RESOURCE_INITIALIZATION
        appUpdateState.isActiveUpdate() -> HomeOperationStatusKind.APP_UPDATE_PROGRESS
        resourceUpdateState.isActiveUpdate() -> HomeOperationStatusKind.RESOURCE_UPDATE_PROGRESS
        appUpdate != null || resourceUpdate != null -> HomeOperationStatusKind.UPDATE_AVAILABLE
        else -> null
    }
    val confirmUpdate = {
        if (appUpdate != null) {
            updateViewModel.confirmAppDownload(appUpdate.version)
        } else {
            updateViewModel.confirmResourceDownload()
        }
        updateViewModel.dismissStartupDialog()
        updateViewModel.dismissAppCheckResult()
        updateViewModel.dismissResourceCheckResult()
    }
    val dismissUpdate = {
        updateViewModel.dismissStartupDialog()
        updateViewModel.dismissAppCheckResult()
        updateViewModel.dismissResourceCheckResult()
    }

    Column(
        modifier = if (resourceInitState is ResourceInitState.Ready) {
            Modifier.benchmarkTestTag("resource_init_ready")
        } else {
            Modifier
        },
    ) {
        AnimatedContent(
            targetState = statusKind,
            transitionSpec = {
                (fadeIn(tween(220)) + expandVertically(expandFrom = Alignment.Top))
                    .togetherWith(
                        fadeOut(tween(160)) + shrinkVertically(shrinkTowards = Alignment.Top),
                    )
                    .using(SizeTransform(clip = false))
            },
            contentKey = { it },
            label = "homeOperationStatus",
        ) { kind ->
            if (kind != null) {
                Column {
                    HomeOperationStatusContent(
                        kind = kind,
                        extracting = extracting,
                        appUpdateState = appUpdateState,
                        resourceUpdateState = resourceUpdateState,
                        appUpdate = appUpdate,
                        resourceUpdate = resourceUpdate,
                        onConfirmUpdate = confirmUpdate,
                        onDismissUpdate = dismissUpdate,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        UpdateCard(viewModel = updateViewModel)
    }
}

private enum class HomeOperationStatusKind {
    RESOURCE_INITIALIZATION,
    APP_UPDATE_PROGRESS,
    RESOURCE_UPDATE_PROGRESS,
    UPDATE_AVAILABLE,
}

@Composable
private fun HomeOperationStatusContent(
    kind: HomeOperationStatusKind,
    extracting: ResourceInitState.Extracting?,
    appUpdateState: UpdateProcessState,
    resourceUpdateState: UpdateProcessState,
    appUpdate: UpdateInfo?,
    resourceUpdate: UpdateInfo?,
    onConfirmUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    when (kind) {
        HomeOperationStatusKind.RESOURCE_INITIALIZATION -> {
            if (extracting != null) {
                ResourceInitializationStatusCard(extracting)
            }
        }
        HomeOperationStatusKind.APP_UPDATE_PROGRESS -> {
            UpdateProgressStatusCard(state = appUpdateState, isAppUpdate = true)
        }
        HomeOperationStatusKind.RESOURCE_UPDATE_PROGRESS -> {
            UpdateProgressStatusCard(state = resourceUpdateState, isAppUpdate = false)
        }
        HomeOperationStatusKind.UPDATE_AVAILABLE -> {
            UpdateAvailableStatusCard(
                appUpdate = appUpdate,
                resourceUpdate = resourceUpdate,
                onConfirm = onConfirmUpdate,
                onDismiss = onDismissUpdate,
            )
        }
    }
}

private fun UpdateProcessState.isActiveUpdate(): Boolean =
    this is UpdateProcessState.Downloading ||
        this is UpdateProcessState.Extracting ||
        this is UpdateProcessState.Installing

@Composable
private fun ResourceInitializationStatusCard(extracting: ResourceInitState.Extracting) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .benchmarkTestTag("resource_init_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.resource_init_in_progress_title),
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { extracting.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${extracting.extractedCount} / ${extracting.totalCount} (${extracting.progress}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun UpdateAvailableStatusCard(
    appUpdate: UpdateInfo?,
    resourceUpdate: UpdateInfo?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .benchmarkTestTag("home_update_status_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.dialog_update_found_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            appUpdate?.let {
                Text(
                    text = stringResource(R.string.dialog_update_app_version_line, it.version),
                    style = MaterialTheme.typography.bodyMedium,
                )
                it.releaseNote?.takeIf(String::isNotBlank)?.let { releaseNote ->
                    Text(
                        text = releaseNote,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            resourceUpdate?.let {
                Text(
                    text = stringResource(
                        R.string.dialog_update_resource_version_line,
                        ResourceDownloader.formatVersionForDisplay(it.version),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_update_dismiss))
                }
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.dialog_update_confirm))
                }
            }
        }
    }
}

@Composable
private fun UpdateProgressStatusCard(
    state: UpdateProcessState,
    isAppUpdate: Boolean,
) {
    val progress = when (state) {
        is UpdateProcessState.Downloading -> state.progress
        is UpdateProcessState.Extracting -> state.progress
        else -> null
    }
    val title = when (state) {
        is UpdateProcessState.Downloading -> stringResource(
            if (isAppUpdate) R.string.update_progress_app_downloading
            else R.string.update_progress_resource_downloading,
            state.progress.toString(),
        )
        is UpdateProcessState.Extracting -> stringResource(
            R.string.update_progress_resource_extracting,
            state.progress.toString(),
        )
        is UpdateProcessState.Installing -> stringResource(R.string.update_progress_app_installing)
        else -> return
    }
    val detail = when (state) {
        is UpdateProcessState.Downloading -> state.speed
        is UpdateProcessState.Extracting -> "${state.current}/${state.total}"
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .benchmarkTestTag("home_update_status_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                detail?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ScreenInfoCard(
    screenWidth: Int,
    screenHeight: Int,
    serviceStatusColor: StatusColorType,
    serviceStatusText: UiText,
    serviceStatusLoading: Boolean
) {
    val serviceStatusLabel = serviceStatusText.asString()
    val containerColor = when (serviceStatusColor) {
        StatusColorType.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
        StatusColorType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        StatusColorType.ERROR -> MaterialTheme.colorScheme.errorContainer
        StatusColorType.NEUTRAL -> MaterialTheme.colorScheme.surfaceBright
    }
    val contentColor = when (serviceStatusColor) {
        StatusColorType.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
        StatusColorType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        StatusColorType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        StatusColorType.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (serviceStatusLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = contentColor,
                )
            } else {
                Icon(
                    imageVector = if (serviceStatusColor == StatusColorType.ERROR) {
                        Icons.Rounded.Warning
                    } else {
                        Icons.Rounded.CheckCircleOutline
                    },
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(
                modifier = Modifier.padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = serviceStatusLabel,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = contentColor,
                )
                Text(
                    text = stringResource(
                        R.string.home_screen_resolution_summary,
                        screenWidth,
                        screenHeight,
                    ),
                    style = MaterialTheme.typography.bodySmallEmphasized,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun RuntimeInfoSection(
    runMode: RunMode,
    onRunModeSelected: (RunMode) -> Unit,
    changeEnabled: Boolean,
    permissionState: PermissionState,
    isGranting: Boolean,
    onRequestShizukuAccess: () -> Unit,
    remoteServiceActive: Boolean,
    isLoading: Boolean,
    shizukuShortcutEnabled: Boolean,
    onOpenShizuku: () -> Unit,
    onCloseRemoteService: () -> Unit,
) {
    val context = LocalContext.current
    SegmentedSettingsGroup {
            item {
                SettingDropdown(
                    title = stringResource(R.string.home_run_mode_title),
                    selected = runMode,
                    options = RunMode.entries,
                    optionLabel = { context.runModeDisplayName(it) },
                    onSelected = onRunModeSelected,
                    icon = null,
                    enabled = changeEnabled,
                    singleLine = true,
                    singleLineActionStyle = true,
                )
            }
            item {
                SettingRow(
                    title = stringResource(R.string.home_shizuku_status_title),
                    icon = null,
                    onClick = if (!permissionState.shizuku && !isGranting) {
                        onRequestShizukuAccess
                    } else {
                        null
                    },
                    trailing = {
                        Box(
                            modifier = Modifier.widthIn(min = 64.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            if (isGranting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = stringResource(
                                        if (permissionState.shizuku) {
                                            R.string.home_permission_granted
                                        } else {
                                            R.string.home_permission_request
                                        },
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (permissionState.shizuku) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }
                    },
                )
            }
            if (shizukuShortcutEnabled) {
                item {
                    SettingActionButton(
                        title = stringResource(R.string.home_btn_open_shizuku),
                        icon = Icons.Rounded.PhoneAndroid,
                        onClick = onOpenShizuku,
                        enabled = !isLoading,
                    )
                }
            }
            item {
                SettingActionButton(
                    title = stringResource(R.string.home_btn_close_service),
                    icon = Icons.Rounded.Refresh,
                    onClick = onCloseRemoteService,
                    enabled = remoteServiceActive && !isLoading,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            }
    }
}

@Composable
private fun ForegroundModeSection(
    overlayControlMode: OverlayControlMode,
    isShowControlOverlay: Boolean,
    isLoading: Boolean,
    onChangeTo16x9Resolution: () -> Unit,
    onResetResolution: () -> Unit,
    onControlOverlayModeChanged: (OverlayControlMode) -> Unit,
    onToggleOverlay: () -> Unit
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.home_resolution_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    Button(
                        onClick = onChangeTo16x9Resolution,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_resolution_apply_16_9),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onResetResolution,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_resolution_reset),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_overlay_mode_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (overlayControlMode == OverlayControlMode.ACCESSIBILITY)
                            stringResource(R.string.home_overlay_mode_accessibility_desc)
                        else
                            stringResource(R.string.home_overlay_mode_floatball_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = context.overlayControlModeDisplayName(overlayControlMode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = overlayControlMode == OverlayControlMode.ACCESSIBILITY,
                        onCheckedChange = { isAccessibility ->
                            onControlOverlayModeChanged(
                                if (isAccessibility) OverlayControlMode.ACCESSIBILITY
                                else OverlayControlMode.FLOAT_BALL
                            )
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onToggleOverlay,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isShowControlOverlay)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            ),
            shape = MaterialTheme.shapes.large,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isShowControlOverlay)
                        stringResource(R.string.home_overlay_close)
                    else
                        stringResource(R.string.home_overlay_open),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
