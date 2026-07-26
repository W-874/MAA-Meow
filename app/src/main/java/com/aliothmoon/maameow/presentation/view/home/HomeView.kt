package com.aliothmoon.maameow.presentation.view.home

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.datasource.ResourceDownloader
import com.aliothmoon.maameow.data.permission.PermissionState
import com.aliothmoon.maameow.domain.models.OverlayControlMode
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.domain.state.ResourceInitState
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.presentation.components.AdaptiveTaskPromptDialog
import com.aliothmoon.maameow.presentation.components.PermissionStatusRow
import com.aliothmoon.maameow.presentation.components.ShizukuReadinessGate
import com.aliothmoon.maameow.presentation.components.ChangelogDialog
import com.aliothmoon.maameow.presentation.components.ResourceInitDialog
import com.aliothmoon.maameow.presentation.components.SectionHeader
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.components.SettingDropdown
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.UpdateCard
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
import dev.jeziellago.compose.markdowntext.MarkdownText
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
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionState by permissionManager.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val (width, height) = Misc.getScreenSize(context)

    val startupDialog by updateViewModel.startupUpdateDialog.collectAsStateWithLifecycle()

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
    LaunchedEffect(uiState.resourceInitState) {
        if (uiState.resourceInitState is ResourceInitState.Ready) {
            updateViewModel.refreshResourceVersion()
            updateViewModel.checkPendingChangelog()
            updateViewModel.checkUpdatesOnStartup()
        }
    }

    // 资源初始化弹窗
    ResourceInitDialog(
        state = uiState.resourceInitState,
        onRetry = { viewModel.onTryResourceInit() }
    )

    // 更新公告弹窗
    val changelogDialog by updateViewModel.changelogDialog.collectAsStateWithLifecycle()
    changelogDialog?.let {
        ChangelogDialog(
            content = it,
            onDismiss = { updateViewModel.dismissChangelog() }
        )
    }

    // 发现更新弹窗
    startupDialog?.let { result ->
        val appVersionLine = result.appUpdate?.let {
            stringResource(R.string.dialog_update_app_version_line, it.version)
        }.orEmpty()
        val resourceMessage = result.resourceUpdate?.let {
            val display = ResourceDownloader.formatVersionForDisplay(it.version)
            stringResource(R.string.update_confirm_message_resource, display)
        }.orEmpty()
        val releaseNote = result.appUpdate?.releaseNote
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.dialog_update_found_title),
            icon = Icons.Rounded.Info,
            confirmText = stringResource(R.string.dialog_update_confirm),
            confirmColor = Color(0xFF4CAF50),
            dismissText = stringResource(R.string.dialog_update_dismiss),
            landscapeAdaptive = true,
            onConfirm = {
                if (result.appUpdate != null) {
                    updateViewModel.confirmAppDownload(result.appUpdate.version)
                } else {
                    updateViewModel.confirmResourceDownload()
                }
                updateViewModel.dismissStartupDialog()
            },
            onDismissRequest = { updateViewModel.dismissStartupDialog() },
            content = {
                Column {
                    Text(
                        text = appVersionLine + resourceMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!releaseNote.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MarkdownText(
                            markdown = releaseNote,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        )
    }

    if (uiState.showRunModeUnsupportedDialog) {
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.dialog_run_mode_unsupported_title),
            message = uiState.runModeUnsupportedMessage.asString(),
            confirmText = stringResource(R.string.common_i_got_it),
            dismissText = null,
            onConfirm = { viewModel.onDismissRunModeUnsupportedDialog() },
            onDismissRequest = { viewModel.onDismissRunModeUnsupportedDialog() }
        )
    }

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
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ScreenInfoCard(
                        screenWidth = width,
                        screenHeight = height,
                        serviceStatusColor = uiState.serviceStatusColor,
                        serviceStatusText = uiState.serviceStatusText,
                        serviceStatusLoading = uiState.serviceStatusLoading
                    )
                }

                item {
                    UpdateCard(viewModel = updateViewModel)
                }

                item {
                    RuntimeInfoSection(
                        runMode = uiState.runMode,
                        onRunModeSelected = {
                            viewModel.onRunModeChange(it == RunMode.BACKGROUND)
                        },
                        changeEnabled = viewModel.checkRunModeChangeEnabled(),
                        permissionState = permissionState,
                        isGranting = uiState.isGranting,
                        onRequestShizukuAccess = { viewModel.onRequestShizukuAccess() },
                        remoteServiceActive = uiState.remoteServiceActive,
                        isLoading = uiState.isLoading,
                        onCloseRemoteService = { viewModel.onToggleRemoteService() },
                    )
                }

                if (uiState.runMode == RunMode.FOREGROUND) {
                    item {
                        ForegroundModeSection(
                            overlayControlMode = uiState.overlayControlMode,
                            isShowControlOverlay = uiState.isShowControlOverlay,
                            isLoading = uiState.isLoading,
                            onChangeTo16x9Resolution = { viewModel.onChangeTo16x9Resolution(context) },
                            onResetResolution = { viewModel.onResetResolution() },
                            onControlOverlayModeChanged = { viewModel.onControlOverlayModeChanged(it) },
                            onToggleOverlay = {
                                if (uiState.isShowControlOverlay) {
                                    Timber.d("关闭悬浮窗")
                                    viewModel.onStopControlOverlay()
                                } else {
                                    Timber.d("开启悬浮窗模式")
                                    viewModel.onStartControlOverlay()
                                }
                            }
                        )
                    }
                }

                item {
                    Column {
                        SectionHeader(stringResource(R.string.home_learn_more_title))
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
        }

        ShizukuReadinessGate()
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
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
    onCloseRemoteService: () -> Unit,
) {
    val context = LocalContext.current
    Column {
        SectionHeader(stringResource(R.string.home_runtime_info_section))
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
                )
            }
            item {
                PermissionStatusRow(
                    title = stringResource(R.string.home_shizuku_status_title),
                    granted = permissionState.shizuku,
                    onClick = onRequestShizukuAccess,
                    isLoading = isGranting,
                )
            }
            item {
                SettingRow(
                    title = stringResource(R.string.home_btn_close_service),
                    titleColor = MaterialTheme.colorScheme.onError,
                    containerColor = MaterialTheme.colorScheme.error,
                    leadingColor = MaterialTheme.colorScheme.onError,
                    icon = Icons.Rounded.Refresh,
                    enabled = remoteServiceActive && !isLoading,
                    onClick = onCloseRemoteService,
                )
            }
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
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
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
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
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
