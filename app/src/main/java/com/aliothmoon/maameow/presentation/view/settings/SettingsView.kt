package com.aliothmoon.maameow.presentation.view.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.aliothmoon.maameow.presentation.benchmarkTestTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.BuildConfig
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.presentation.navigation.LocalMainBottomBarPadding
import com.aliothmoon.maameow.constant.DefaultDisplayConfig
import com.aliothmoon.maameow.constant.OFFICIAL_SHIZUKU_PACKAGE
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.data.model.update.UpdateChannel
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.RemoteBackend
import com.aliothmoon.maameow.domain.service.ResourceInitService
import com.aliothmoon.maameow.domain.state.ResourceInitState
import com.aliothmoon.maameow.manager.ShizukuInstallHelper
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.presentation.components.AdaptiveTaskPromptDialog
import com.aliothmoon.maameow.presentation.components.ExpressiveSwitch
import com.aliothmoon.maameow.presentation.components.ITextField
import com.aliothmoon.maameow.presentation.components.PermissionStatusRow
import com.aliothmoon.maameow.presentation.components.ReInitializeConfirmDialog
import com.aliothmoon.maameow.presentation.components.ResourceInitDialog
import com.aliothmoon.maameow.presentation.components.SectionHeader
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.components.SettingDropdown
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.UpdateSourceSettings
import com.aliothmoon.maameow.presentation.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.presentation.viewmodel.UpdateViewModel
import com.aliothmoon.maameow.theme.MaaDesignTokens
import com.aliothmoon.maameow.utils.Misc
import com.aliothmoon.maameow.utils.i18n.LocaleBootstrap.resolveSelectedLanguage
import com.aliothmoon.maameow.utils.i18n.resolve
import com.aliothmoon.maameow.utils.i18n.remoteBackendPermissionLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun SettingsView(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    updateViewModel: UpdateViewModel = koinViewModel(),
    resourceInitService: ResourceInitService = koinInject(),
    permissionManager: PermissionManager = koinInject(),
    appSettingsManager: AppSettingsManager = koinInject(),
) {
    val mainBottomBarPadding = LocalMainBottomBarPadding.current
    val resourceInitState by resourceInitService.state.collectAsStateWithLifecycle()
    val startupBackend by viewModel.startupBackend.collectAsStateWithLifecycle()
    val shizukuShortcutEnabled by viewModel.shizukuShortcutEnabled.collectAsStateWithLifecycle()
    val shizukuLaunchPackage by viewModel.shizukuLaunchPackage.collectAsStateWithLifecycle()
    val tasksOverrideEnabled by viewModel.tasksOverrideEnabled.collectAsStateWithLifecycle()
    val taskNotificationStyle by appSettingsManager.taskNotificationStyle.collectAsStateWithLifecycle()
    val miIslandBypass by appSettingsManager.miIslandBypassRestriction.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
    val showRestartDialog by viewModel.showRestartDialog.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.let { viewModel.exportConfig(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.let { viewModel.importConfig(it) }
    }

    var showShizukuAppPicker by remember { mutableStateOf(false) }
    var shizukuAppPickerLoadKey by remember { mutableIntStateOf(0) }
    var shizukuAppSearch by remember { mutableStateOf("") }
    var shizukuAppOptions by remember { mutableStateOf<List<ShizukuLaunchAppOption>?>(null) }
    var shizukuAppLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(showShizukuAppPicker, shizukuAppPickerLoadKey) {
        if (!showShizukuAppPicker) return@LaunchedEffect

        shizukuAppLoadFailed = false
        shizukuAppOptions = null
        shizukuAppOptions = try {
            withContext(Dispatchers.IO) {
                loadShizukuLaunchApps(context.applicationContext)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            shizukuAppLoadFailed = true
            emptyList()
        }
    }

    backupMessage?.let { msg ->
        Toast.makeText(context, msg.resolve(context), Toast.LENGTH_SHORT).show()
        viewModel.clearBackupMessage()
    }

    var showReInitConfirm by remember { mutableStateOf(false) }
    var showDebugModeConfirm by remember { mutableStateOf(false) }
    var showRunScheduleWhenLockedConfirm by remember { mutableStateOf(false) }

    if (showRestartDialog) {
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.dialog_import_success_title),
            message = stringResource(R.string.dialog_import_success_message),
            icon = Icons.Rounded.Build,
            confirmText = stringResource(R.string.common_restart_now),
            dismissText = stringResource(R.string.common_restart_later),
            onConfirm = { viewModel.confirmRestart() },
            onDismissRequest = { viewModel.dismissRestartDialog() }
        )
    }

    if (showReInitConfirm) {
        ReInitializeConfirmDialog(
            onConfirm = {
                showReInitConfirm = false
                coroutineScope.launch {
                    resourceInitService.reInitialize()
                }
            },
            onDismiss = { showReInitConfirm = false }
        )
    }

    if (showDebugModeConfirm) {
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.dialog_enable_debug_title),
            message = stringResource(R.string.dialog_enable_debug_message),
            onConfirm = {
                showDebugModeConfirm = false
                viewModel.setDebugMode(true)
            },
            onDismissRequest = { showDebugModeConfirm = false },
            confirmText = stringResource(R.string.common_confirm_restart),
            dismissText = stringResource(R.string.common_cancel),
            icon = Icons.Rounded.Build
        )
    }

    if (showRunScheduleWhenLockedConfirm) {
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.dialog_run_schedule_when_locked_title),
            message = stringResource(R.string.dialog_run_schedule_when_locked_message),
            onConfirm = {
                showRunScheduleWhenLockedConfirm = false
                viewModel.setRunScheduleWhenLocked(true)
            },
            onDismissRequest = { showRunScheduleWhenLockedConfirm = false },
            confirmText = stringResource(R.string.dialog_run_schedule_when_locked_confirm),
            dismissText = stringResource(R.string.common_cancel),
            icon = Icons.Rounded.Build
        )
    }

    if (resourceInitState is ResourceInitState.Extracting) {
        ResourceInitDialog(
            state = resourceInitState,
            onRetry = {}
        )
    }

    if (showShizukuAppPicker) {
        val searchText = shizukuAppSearch.trim()
        val filteredOptions = shizukuAppOptions
            ?.filter { option ->
                searchText.isBlank() ||
                        option.label.contains(searchText, ignoreCase = true) ||
                        option.packageName.contains(searchText, ignoreCase = true)
            }
            .orEmpty()

        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.settings_shizuku_launch_app_picker_title),
            icon = Icons.Rounded.Build,
            confirmText = stringResource(R.string.common_close),
            dismissText = "",
            onConfirm = { showShizukuAppPicker = false },
            onDismissRequest = { showShizukuAppPicker = false },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when {
                        shizukuAppOptions == null -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.settings_shizuku_launch_app_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        shizukuAppLoadFailed -> {
                            Text(
                                text = stringResource(R.string.settings_shizuku_launch_app_picker_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        else -> {
                            ITextField(
                                value = shizukuAppSearch,
                                onValueChange = { shizukuAppSearch = it },
                                placeholder = stringResource(R.string.settings_shizuku_launch_app_search_hint),
                                singleLine = true
                            )

                            if (filteredOptions.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.settings_shizuku_launch_app_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 320.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filteredOptions, key = { it.packageName }) { option ->
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        viewModel.setShizukuLaunchPackage(option.packageName)
                                                        showShizukuAppPicker = false
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = option.label,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = option.packageName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        val contentColor = MaterialTheme.colorScheme.onSurface

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .benchmarkTestTag("settings_list"),
            contentPadding = PaddingValues(
                start = MaaDesignTokens.Spacing.listHorizontal,
                end = MaaDesignTokens.Spacing.listHorizontal,
                top = paddingValues.calculateTopPadding() + MaaDesignTokens.Spacing.sm,
                bottom = paddingValues.calculateBottomPadding() +
                    mainBottomBarPadding + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.sectionGap),
        ) {
            // 更新管理
            item {
                SectionHeader(stringResource(R.string.settings_section_update))
                SegmentedSettingsGroup {
                    item { SettingClickItem(
                        title = stringResource(R.string.settings_reinit_resource_title),
                        description = stringResource(R.string.settings_reinit_resource_desc),
                        contentColor = contentColor,
                        icon = Icons.Rounded.RestartAlt,
                    ) {
                        showReInitConfirm = true
                    } }
                    item {
                        val autoCheckUpdate by viewModel.autoCheckUpdate.collectAsStateWithLifecycle()
                        SettingSwitchItem(
                        title = stringResource(R.string.settings_auto_check_update_title),
                        description = stringResource(R.string.settings_auto_check_update_desc),
                        contentColor = contentColor,
                        checked = autoCheckUpdate,
                        icon = Icons.Rounded.Update,
                        onCheckedChange = { viewModel.setAutoCheckUpdate(it) }
                    ) }
                    item {
                        val autoDownloadUpdate by viewModel.autoDownloadUpdate.collectAsStateWithLifecycle()
                        val autoCheckUpdate by viewModel.autoCheckUpdate.collectAsStateWithLifecycle()
                        SettingSwitchItem(
                        title = stringResource(R.string.settings_auto_download_update_title),
                        description = stringResource(R.string.settings_auto_download_update_desc),
                        contentColor = contentColor,
                        checked = autoDownloadUpdate,
                        enabled = autoCheckUpdate,
                        icon = Icons.Rounded.Download,
                        onCheckedChange = { viewModel.setAutoDownloadUpdate(it) }
                    ) }
                    item {
                        val updateChannel by viewModel.updateChannel.collectAsStateWithLifecycle()
                        SettingChannelItem(
                        contentColor = contentColor,
                        selectedChannel = updateChannel,
                        onChannelSelected = { viewModel.setUpdateChannel(it) }
                    ) }
                    item { UpdateSourceSettings(viewModel = updateViewModel) }
                }
            }

            // 日志
            item {
                SectionHeader(stringResource(R.string.settings_section_log))
                SegmentedSettingsGroup {
                    item { SettingClickItem(
                        title = stringResource(R.string.settings_log_history_title),
                        description = stringResource(R.string.settings_log_history_desc),
                        contentColor = contentColor,
                        icon = Icons.Rounded.History,
                    ) {
                        navController.navigate("log_history")
                    } }
                    item {
                        val debugMode by viewModel.debugMode.collectAsStateWithLifecycle()
                        SettingSwitchItem(
                        title = stringResource(R.string.settings_debug_mode_title),
                        description = stringResource(R.string.settings_debug_mode_desc),
                        contentColor = contentColor,
                        checked = debugMode,
                        icon = Icons.Rounded.Code,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showDebugModeConfirm = true
                            } else {
                                viewModel.setDebugMode(false)
                            }
                        }
                    ) }
                }
            }

            // 显示设置
            item {
                SectionHeader(stringResource(R.string.settings_section_display))
                SegmentedSettingsGroup {
                    item {
                        val language by viewModel.language.collectAsStateWithLifecycle()
                        SettingLanguageItem(
                        contentColor = contentColor,
                        selectedLanguage = language,
                        onLanguageSelected = { viewModel.setLanguage(it) }
                    ) }
                    item {
                        val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                        SettingThemeModeItem(
                        selectedMode = themeMode,
                        onModeSelected = { viewModel.setThemeMode(it) },
                    ) }
                    item {
                        val fontSizeScale by viewModel.fontSizeScale.collectAsStateWithLifecycle()
                        FontSizeSetting(
                        contentColor = contentColor,
                        fontSizeScale = fontSizeScale,
                        onFontSizeScaleChanged = { viewModel.setFontSizeScale(it) },
                    ) }
                }
            }

            // 权限管理
            item {
                SettingsPermissionSection(permissionManager)
            }

            // 其他设置
            item {
                SectionHeader(stringResource(R.string.settings_section_other))
                SegmentedSettingsGroup {
                    if (startupBackend == RemoteBackend.SHIZUKU) {
                        item { SettingSwitchItem(
                            title = stringResource(R.string.settings_shizuku_launch_mode_title),
                            description = stringResource(R.string.settings_shizuku_launch_mode_desc),
                            contentColor = contentColor,
                            checked = shizukuShortcutEnabled,
                            icon = Icons.Rounded.PhoneAndroid,
                            onCheckedChange = { viewModel.setShizukuShortcutEnabled(it) }
                        ) }
                        if (shizukuShortcutEnabled) {
                            item {
                                val appName = ShizukuInstallHelper.getLaunchAppLabel(context, shizukuLaunchPackage)
                                val description = if (shizukuLaunchPackage == OFFICIAL_SHIZUKU_PACKAGE) {
                                    stringResource(R.string.settings_shizuku_launch_app_default_desc)
                                } else {
                                    stringResource(R.string.settings_shizuku_launch_app_selected_desc, appName ?: shizukuLaunchPackage)
                                }
                                SettingClickItem(
                                    title = stringResource(R.string.settings_shizuku_launch_app_title),
                                    description = description,
                                    contentColor = contentColor,
                                    icon = Icons.Rounded.PhoneAndroid,
                                ) {
                                    shizukuAppSearch = ""
                                    shizukuAppPickerLoadKey += 1
                                    showShizukuAppPicker = true
                                }
                            }
                            item { SettingClickItem(
                                    title = stringResource(R.string.settings_shizuku_launch_app_reset_title),
                                    description = stringResource(R.string.settings_shizuku_launch_app_reset_desc),
                                    contentColor = contentColor,
                                    icon = Icons.Rounded.RestartAlt,
                                ) {
                                    viewModel.setShizukuLaunchPackage(OFFICIAL_SHIZUKU_PACKAGE)
                                } }
                        }
                    }
                    item {
                        val backgroundResolution by viewModel.backgroundResolution.collectAsStateWithLifecycle()
                        SettingBackgroundResolutionItem(
                        contentColor = contentColor,
                        selectedPreference = backgroundResolution,
                        onPreferenceSelected = { viewModel.setBackgroundResolution(it) }
                    ) }
                    item {
                        val skipShizukuCheck by viewModel.skipShizukuCheck.collectAsStateWithLifecycle()
                        SettingSwitchItem(
                        title = stringResource(R.string.settings_skip_shizuku_check),
                        contentColor = contentColor,
                        checked = skipShizukuCheck,
                        enabled = startupBackend == RemoteBackend.SHIZUKU,
                        icon = Icons.Rounded.Security,
                        onCheckedChange = { viewModel.setSkipShizukuCheck(it) }
                    ) }
                    item {
                        val deploymentWithPause by viewModel.deploymentWithPause.collectAsStateWithLifecycle()
                        SettingSwitchItem(
                        title = stringResource(R.string.settings_deployment_with_pause),
                        description = stringResource(R.string.settings_deployment_with_pause_tip),
                        contentColor = contentColor,
                        checked = deploymentWithPause,
                        icon = Icons.Rounded.Build,
                        onCheckedChange = { viewModel.setDeploymentWithPause(it) }
                    ) }
                    item {
                        val forceFullscreenOnVirtualDisplay by viewModel.forceFullscreenOnVirtualDisplay.collectAsStateWithLifecycle()
                        SettingSwitchItem(
                        title = stringResource(R.string.settings_force_fullscreen_on_virtual_display),
                        contentColor = contentColor,
                        checked = forceFullscreenOnVirtualDisplay,
                        icon = Icons.Rounded.AspectRatio,
                        onCheckedChange = { viewModel.setForceFullscreenOnVirtualDisplay(it) }
                    ) }
                    item {
                        val allowForegroundScheduledTask by viewModel.allowForegroundScheduledTask.collectAsStateWithLifecycle()
                        SettingSwitchItem(
                        title = stringResource(R.string.settings_allow_foreground_scheduled_task),
                        contentColor = contentColor,
                        checked = allowForegroundScheduledTask,
                        icon = Icons.Rounded.Schedule,
                        onCheckedChange = { viewModel.setAllowForegroundScheduledTask(it) }
                    ) }
                    item {
                        val runScheduleWhenLocked by viewModel.runScheduleWhenLocked.collectAsStateWithLifecycle()
                        SettingSwitchItem(
                        title = stringResource(R.string.settings_run_schedule_when_locked),
                        contentColor = contentColor,
                        checked = runScheduleWhenLocked,
                        icon = Icons.Rounded.Lock,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showRunScheduleWhenLockedConfirm = true
                            } else {
                                viewModel.setRunScheduleWhenLocked(false)
                            }
                        }
                    ) }
                    item {
                        SettingSwitchItem(
                        title = stringResource(R.string.settings_tasks_override_title),
                        description = stringResource(R.string.settings_tasks_override_desc),
                        contentColor = contentColor,
                        checked = tasksOverrideEnabled,
                        icon = Icons.Rounded.Tune,
                        onCheckedChange = { viewModel.setTasksOverrideEnabled(it) }
                    ) }
                    if (tasksOverrideEnabled) {
                        item { SettingClickItem(
                                title = stringResource(R.string.settings_tasks_override_edit_title),
                                contentColor = contentColor,
                                icon = Icons.Rounded.Tune,
                            ) {
                                navController.navigate(Routes.TASK_OVERRIDE_EDITOR)
                            } }
                    }
                }
            }

            // 数据管理
            item {
                SectionHeader(stringResource(R.string.settings_section_data))
                SegmentedSettingsGroup {
                    item { SettingClickItem(
                        title = stringResource(R.string.settings_export_config_title),
                        description = stringResource(R.string.settings_export_config_desc),
                        contentColor = contentColor,
                        icon = Icons.Rounded.Backup,
                    ) {
                        exportLauncher.launch("maameow_config.json")
                    } }
                    item { SettingClickItem(
                        title = stringResource(R.string.settings_import_config_title),
                        description = stringResource(R.string.settings_import_config_desc),
                        contentColor = contentColor,
                        icon = Icons.Rounded.CloudDownload,
                    ) {
                        importLauncher.launch(arrayOf("application/json"))
                    } }
                }
            }

            // 通知
            item {
                SectionHeader(stringResource(R.string.settings_section_notification))
                SegmentedSettingsGroup {
                    item {
                        SettingDropdown(
                            title = stringResource(R.string.notification_task_style),
                            selected = taskNotificationStyle,
                            options = AppSettingsManager.TaskNotificationStyle.entries,
                            optionLabel = {
                                stringResource(
                                    when (it) {
                                        AppSettingsManager.TaskNotificationStyle.ANDROID ->
                                            R.string.notification_task_style_android
                                        AppSettingsManager.TaskNotificationStyle.MI_ISLAND ->
                                            R.string.notification_task_style_mi_island
                                    }
                                )
                            },
                            onSelected = { style ->
                                coroutineScope.launch {
                                    appSettingsManager.setTaskNotificationStyle(style)
                                }
                            },
                            icon = Icons.Rounded.Notifications,
                        )
                    }
                    if (taskNotificationStyle == AppSettingsManager.TaskNotificationStyle.MI_ISLAND) {
                        item {
                            SettingSwitchItem(
                                title = stringResource(R.string.notification_mi_island_bypass),
                                contentColor = contentColor,
                                checked = miIslandBypass,
                                icon = Icons.Rounded.Security,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch {
                                        appSettingsManager.setMiIslandBypassRestriction(enabled)
                                    }
                                },
                            )
                        }
                    }
                    item { SettingClickItem(
                        title = stringResource(R.string.settings_notification_title),
                        description = stringResource(R.string.settings_notification_desc),
                        contentColor = contentColor,
                        icon = Icons.Rounded.Notifications,
                    ) {
                        navController.navigate(Routes.NOTIFICATION)
                    } }
                }
            }

            // 成就
            item {
                SectionHeader(stringResource(R.string.settings_section_achievement))
                SegmentedSettingsGroup {
                    item { SettingClickItem(
                        title = stringResource(R.string.settings_achievement_title),
                        description = stringResource(R.string.settings_achievement_desc),
                        contentColor = contentColor,
                        icon = Icons.Rounded.EmojiEvents,
                    ) {
                        navController.navigate(Routes.ACHIEVEMENT)
                    } }
                    item {
                        val showAchievementSnackbar by viewModel.showAchievementSnackbar.collectAsStateWithLifecycle()
                        SettingSwitchItem(
                        title = stringResource(R.string.settings_achievement_snackbar_title),
                        description = stringResource(R.string.settings_achievement_snackbar_desc),
                        contentColor = contentColor,
                        checked = showAchievementSnackbar,
                        icon = Icons.Rounded.Campaign,
                        onCheckedChange = { viewModel.setShowAchievementSnackbar(it) }
                    ) }
                    if (BuildConfig.DEBUG) {
                        item { SettingClickItem(
                            title = stringResource(R.string.settings_achievement_debug_title),
                            description = stringResource(R.string.settings_achievement_debug_desc),
                            contentColor = contentColor,
                            icon = Icons.Rounded.BugReport,
                        ) {
                            navController.navigate(Routes.ACHIEVEMENT_DEBUG)
                        } }
                    }
                }
            }

            // 关于
            item {
                SectionHeader(stringResource(R.string.settings_section_about))
                SegmentedSettingsGroup {
                    item { SettingClickItem(
                        title = stringResource(R.string.settings_about_page_title),
                        description = stringResource(R.string.settings_about_page_desc, BuildConfig.VERSION_NAME),
                        contentColor = contentColor,
                        icon = Icons.Rounded.Info,
                    ) {
                        navController.navigate(Routes.ABOUT)
                    } }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingThemeModeItem(
    selectedMode: AppSettingsManager.ThemeMode,
    onModeSelected: (AppSettingsManager.ThemeMode) -> Unit,
) {
    SettingDropdown(
        title = stringResource(R.string.settings_theme_title),
        selected = selectedMode,
        options = AppSettingsManager.ThemeMode.entries,
        optionLabel = {
            stringResource(
                when (it) {
                    AppSettingsManager.ThemeMode.SYSTEM -> R.string.settings_theme_system
                    AppSettingsManager.ThemeMode.WHITE -> R.string.settings_theme_white
                    AppSettingsManager.ThemeMode.DARK -> R.string.settings_theme_dark
                    AppSettingsManager.ThemeMode.PURE_DARK -> R.string.settings_theme_pure_dark
                }
            )
        },
        onSelected = onModeSelected,
        icon = Icons.Rounded.Palette,
    )
}

@Composable
private fun SettingClickItem(
    title: String,
    description: String = "",
    contentColor: Color,
    icon: ImageVector = Icons.Rounded.TouchApp,
    onClick: () -> Unit
) {
    SettingRow(
        title = title,
        description = description.ifEmpty { null },
        titleColor = contentColor,
        descriptionColor = contentColor.copy(alpha = 0.7f),
        icon = icon,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
            )
        },
        onClick = onClick,
    )
}

/**
 * 字体大小（页面缩放）设置：整数 80~110，默认 100。
 * 松手后才提交全局缩放。滑块下方带实时预览框。
 */
@Composable
private fun FontSizeSetting(
    contentColor: Color,
    fontSizeScale: Int,
    onFontSizeScaleChanged: (Int) -> Unit,
) {
    var sliderValue by remember { mutableFloatStateOf(fontSizeScale.toFloat()) }
    LaunchedEffect(fontSizeScale) {
        sliderValue = fontSizeScale.toFloat()
    }
    val current = sliderValue.roundToInt()
        .coerceIn(AppSettingsManager.FONT_SIZE_SCALE_MIN, AppSettingsManager.FONT_SIZE_SCALE_MAX)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SettingRow(
            title = stringResource(R.string.settings_font_size_title),
            description = stringResource(R.string.settings_font_size_summary),
            titleColor = contentColor,
            descriptionColor = contentColor.copy(alpha = 0.7f),
            icon = Icons.Rounded.Tune,
            trailing = {
                Text(
                    text = current.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor.copy(alpha = 0.7f),
                )
            },
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    onFontSizeScaleChanged(
                        sliderValue.roundToInt().coerceIn(
                            AppSettingsManager.FONT_SIZE_SCALE_MIN,
                            AppSettingsManager.FONT_SIZE_SCALE_MAX
                        )
                    )
                },
                valueRange = AppSettingsManager.FONT_SIZE_SCALE_MIN.toFloat()..AppSettingsManager.FONT_SIZE_SCALE_MAX.toFloat(),
                steps = 0,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    AppSettingsManager.FONT_SIZE_SCALE_MIN,
                    90,
                    100,
                    AppSettingsManager.FONT_SIZE_SCALE_MAX
                ).forEach { kp ->
                    Text(
                        text = kp.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.5f)
                    )
                }
            }
            // 实时预览框：previewDensity 已被全局缩放（D0 * value/100），
            // 故按 current/value 还原到 D0 * current/100，避免与全局缩放叠加造成重复缩放。
            val previewDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = previewDensity.density * current / fontSizeScale.toFloat(),
                    fontScale = previewDensity.fontScale
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaaDesignTokens.Spacing.sm),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = stringResource(R.string.settings_font_size_preview_text),
                        modifier = Modifier.padding(16.dp),
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    description: String? = null,
    contentColor: Color,
    checked: Boolean,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Rounded.Tune,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingRow(
        title = title,
        description = description,
        titleColor = contentColor,
        descriptionColor = contentColor.copy(alpha = 0.7f),
        icon = icon,
        enabled = enabled,
        trailing = {
            ExpressiveSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
            )
        },
        onClick = { onCheckedChange(!checked) },
    )
}

@Composable
private fun SettingInfoRow(
    label: String,
    value: String,
    contentColor: Color,
    onClick: (() -> Unit)? = null,
) {
    SettingRow(
        title = label,
        titleColor = contentColor,
        icon = Icons.Rounded.Info,
        trailing = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor.copy(alpha = 0.7f)
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun SettingsPermissionSection(permissionManager: PermissionManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val permissionState by permissionManager.state.collectAsStateWithLifecycle()
    val isGrantingPermission by permissionManager.isGranting.collectAsStateWithLifecycle()

    SectionHeader(stringResource(R.string.home_permission_section))
    SegmentedSettingsGroup {
        item {
            PermissionStatusRow(
                title = context.remoteBackendPermissionLabel(permissionState.startupBackend),
                granted = permissionState.remoteAccessGranted,
                isLoading = isGrantingPermission,
                icon = Icons.Rounded.Security,
                onClick = {
                    coroutineScope.launch {
                        val backend = permissionState.startupBackend
                        if (!permissionState.isStartupBackendAvailable(backend)) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.home_toast_backend_unavailable, backend.display),
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@launch
                        }
                        if (!permissionManager.requestRemoteAccess()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.home_toast_backend_auth_failed, backend.display),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                },
            )
        }
        item {
            PermissionStatusRow(
                title = stringResource(R.string.home_permission_overlay),
                granted = permissionState.overlay,
                icon = Icons.Rounded.TouchApp,
                onClick = { coroutineScope.launch { permissionManager.requestOverlay(context) } },
            )
        }
        item {
            PermissionStatusRow(
                title = stringResource(R.string.home_permission_storage),
                granted = permissionState.storage,
                icon = Icons.Rounded.Folder,
                onClick = { coroutineScope.launch { permissionManager.requestStorage(context) } },
            )
        }
        item {
            PermissionStatusRow(
                title = stringResource(R.string.home_permission_battery),
                granted = permissionState.batteryWhitelist,
                grantedText = stringResource(R.string.home_permission_battery_added),
                icon = Icons.Rounded.BatteryChargingFull,
                onClick = { coroutineScope.launch { permissionManager.requestBatteryWhitelist(context) } },
            )
        }
        item {
            PermissionStatusRow(
                title = stringResource(R.string.home_permission_accessibility),
                granted = permissionState.accessibility,
                icon = Icons.Rounded.AccessibilityNew,
                ungrantedText = if (permissionState.remoteAccessGranted) {
                    stringResource(R.string.home_permission_quick_grant)
                } else {
                    stringResource(R.string.home_permission_request)
                },
                onClick = {
                    coroutineScope.launch {
                        if (permissionState.remoteAccessGranted && permissionManager.quickGrantAccessibility()) {
                            return@launch
                        }
                        permissionManager.requestAccessibility(context)
                    }
                },
            )
        }
        item {
            PermissionStatusRow(
                title = stringResource(R.string.home_permission_notification),
                granted = permissionState.notification,
                icon = Icons.Rounded.Notifications,
                onClick = { coroutineScope.launch { permissionManager.requestNotification(context) } },
            )
        }
    }
}

@Composable
private fun SettingChannelItem(
    contentColor: Color,
    selectedChannel: UpdateChannel,
    onChannelSelected: (UpdateChannel) -> Unit
) {
    SettingDropdown(
        title = stringResource(R.string.settings_update_channel_title),
        selected = selectedChannel,
        options = UpdateChannel.entries,
        optionLabel = { stringResource(it.resId) },
        onSelected = onChannelSelected,
        icon = Icons.Rounded.Update,
    )
}

@Composable
private fun SettingBackgroundResolutionItem(
    contentColor: Color,
    selectedPreference: DefaultDisplayConfig.ResolutionPreference,
    onPreferenceSelected: (DefaultDisplayConfig.ResolutionPreference) -> Unit
) {
    SettingDropdown(
        title = stringResource(R.string.settings_background_resolution_title),
        selected = selectedPreference,
        options = DefaultDisplayConfig.ResolutionPreference.entries,
        optionLabel = { if (it == DefaultDisplayConfig.ResolutionPreference.P720) "720p" else "1080p" },
        onSelected = onPreferenceSelected,
        icon = Icons.Rounded.AspectRatio,
    )
}

@Composable
private fun SettingLanguageItem(
    contentColor: Color,
    selectedLanguage: AppSettingsManager.AppLanguage,
    onLanguageSelected: (AppSettingsManager.AppLanguage) -> Unit
) {
    val effectiveSelectedLanguage = resolveSelectedLanguage(selectedLanguage)

    SettingDropdown(
        title = stringResource(R.string.settings_language_title),
        selected = effectiveSelectedLanguage,
        options = listOf(AppSettingsManager.AppLanguage.ZH, AppSettingsManager.AppLanguage.EN),
        optionLabel = {
            stringResource(if (it == AppSettingsManager.AppLanguage.ZH) R.string.settings_language_zh else R.string.settings_language_en)
        },
        onSelected = onLanguageSelected,
        icon = Icons.Rounded.Language,
    )
}

private data class ShizukuLaunchAppOption(
    val label: String,
    val packageName: String
)

private fun loadShizukuLaunchApps(context: Context): List<ShizukuLaunchAppOption> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    // 应用列表查询较慢，调用方应在 IO 线程执行。
    return packageManager.queryIntentActivities(launcherIntent, 0)
        .mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
            val label = resolveInfo.loadLabel(packageManager).toString()
                .takeIf { it.isNotBlank() }
                ?: packageName
            ShizukuLaunchAppOption(label = label, packageName = packageName)
        }
        .distinctBy { it.packageName }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}
