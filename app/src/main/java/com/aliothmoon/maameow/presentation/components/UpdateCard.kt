package com.aliothmoon.maameow.presentation.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.update.UpdateCheckResult
import com.aliothmoon.maameow.data.model.update.UpdateInfo
import com.aliothmoon.maameow.data.model.update.UpdateProcessState
import com.aliothmoon.maameow.data.model.update.UpdateSource
import com.aliothmoon.maameow.presentation.viewmodel.UpdateViewModel
import com.aliothmoon.maameow.utils.Misc
import com.aliothmoon.maameow.utils.i18n.resolve
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * 首页版本状态卡片
 * 展示应用与资源版本，并提供手动检查和更新进度。
 */

@Composable
fun UpdateCard(
    viewModel: UpdateViewModel
) {
    val resourceUpdateState by viewModel.resourceUpdateState.collectAsStateWithLifecycle()
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val resIsChecking by viewModel.resourceChecking.collectAsStateWithLifecycle()
    val appIsChecking by viewModel.appChecking.collectAsStateWithLifecycle()
    val resourceCheckResult by viewModel.resourceCheckResult.collectAsStateWithLifecycle()
    val appCheckResult by viewModel.appCheckResult.collectAsStateWithLifecycle()
    val currentResourceVersion by viewModel.currentResourceVersion.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val resourceUpToDateMessage = stringResource(R.string.update_toast_resource_up_to_date)
    val appUpToDateMessage = stringResource(R.string.update_toast_app_up_to_date)
    val resourceUpdateCompleteMessage = stringResource(R.string.update_toast_resource_update_complete)
    val apkDownloadCompleteMessage = stringResource(R.string.update_toast_apk_download_complete)
    val resourceCheckFailedFormat = stringResource(R.string.update_toast_check_resource_failed)
    val appCheckFailedFormat = stringResource(R.string.update_toast_check_app_failed)
    val resourceUpdateFailedFormat = stringResource(R.string.update_toast_resource_update_failed)

    var resourceErrorMessage by remember { mutableStateOf<String?>(null) }
    var appErrorMessage by remember { mutableStateOf<String?>(null) }

    // ==================== 资源检查结果处理 ====================

    LaunchedEffect(resourceCheckResult) {
        when (val result = resourceCheckResult) {
            is UpdateCheckResult.UpToDate -> {
                Toast.makeText(
                    context,
                    resourceUpToDateMessage,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.dismissResourceCheckResult()
            }

            is UpdateCheckResult.Error -> {
                resourceErrorMessage = resourceCheckFailedFormat.format(result.error.text.resolve(context))
                viewModel.dismissResourceCheckResult()
            }

            else -> {}
        }
    }

    // ==================== 应用检查结果处理 ====================

    LaunchedEffect(appCheckResult) {
        when (val result = appCheckResult) {
            is UpdateCheckResult.UpToDate -> {
                Toast.makeText(
                    context,
                    appUpToDateMessage,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.dismissAppCheckResult()
            }

            is UpdateCheckResult.Error -> {
                appErrorMessage = appCheckFailedFormat.format(result.error.text.resolve(context))
                viewModel.dismissAppCheckResult()
            }

            else -> {}
        }
    }

    // ==================== 下载过程状态处理 ====================

    LaunchedEffect(resourceUpdateState) {
        when (val state = resourceUpdateState) {
            is UpdateProcessState.Failed -> {
                resourceErrorMessage = resourceUpdateFailedFormat.format(state.error.text.resolve(context))
            }

            is UpdateProcessState.Success -> {
                Toast.makeText(
                    context,
                    resourceUpdateCompleteMessage,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.reset()
            }

            else -> {}
        }
    }

    LaunchedEffect(appUpdateState) {
        when (val state = appUpdateState) {
            is UpdateProcessState.Failed -> {
                appErrorMessage = state.error.text.resolve(context)
            }

            is UpdateProcessState.Success -> {
                Toast.makeText(
                    context,
                    apkDownloadCompleteMessage,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetAppUpdate()
            }

            else -> {}
        }
    }

    // ==================== 弹窗 ====================

    // 资源更新确认弹窗
    (resourceCheckResult as? UpdateCheckResult.Available)?.info?.let { updateInfo ->
        UpdateConfirmDialog(
            updateInfo = updateInfo,
            onConfirm = {
                viewModel.dismissResourceCheckResult()
                viewModel.confirmResourceDownload()
            },
            onDismiss = {
                viewModel.dismissResourceCheckResult()
            }
        )
    }

    // 应用更新确认弹窗
    (appCheckResult as? UpdateCheckResult.Available)?.info?.let { updateInfo ->
        AppUpdateConfirmDialog(
            updateInfo = updateInfo,
            currentVersion = viewModel.currentAppVersion,
            onConfirm = {
                viewModel.dismissAppCheckResult()
                viewModel.confirmAppDownload(updateInfo.version)
            },
            onDismiss = {
                viewModel.dismissAppCheckResult()
            }
        )
    }

    // 资源更新错误弹窗
    resourceErrorMessage?.let { message ->
        ErrorDialog(
            message = message,
            onDismiss = {
                resourceErrorMessage = null
                viewModel.reset()
            }
        )
    }

    // 应用更新错误弹窗
    appErrorMessage?.let { message ->
        ErrorDialog(
            message = message,
            onDismiss = {
                appErrorMessage = null
                viewModel.resetAppUpdate()
            }
        )
    }

    // ==================== 状态标记 ====================

    val resIsDownloading = resourceUpdateState is UpdateProcessState.Downloading
    val resIsExtracting = resourceUpdateState is UpdateProcessState.Extracting
    val resIsInstalling = resourceUpdateState is UpdateProcessState.Installing
    val resIsUpdating = resIsDownloading || resIsExtracting || resIsInstalling

    val appIsDownloading = appUpdateState is UpdateProcessState.Downloading
    val appIsInstalling = appUpdateState is UpdateProcessState.Installing
    val appIsUpdating = appIsDownloading || appIsInstalling

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VersionStatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.update_card_app),
                value = viewModel.currentAppVersion,
                checking = appIsChecking,
                updating = appIsUpdating,
                onClick = viewModel::checkAppUpdate,
            )
            VersionStatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.update_card_resource),
                value = currentResourceVersion.ifBlank {
                    stringResource(R.string.home_resource_not_installed)
                },
                checking = resIsChecking,
                updating = resIsUpdating,
                isError = currentResourceVersion.isBlank(),
                onClick = viewModel::checkResourceUpdate,
            )
        }
        AnimatedVisibility(visible = appIsUpdating) {
            AppUpdateProgress(appUpdateState)
        }
        AnimatedVisibility(visible = resIsUpdating) {
            ResourceUpdateProgress(resourceUpdateState)
        }
    }
}

@Composable
private fun VersionStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    checking: Boolean,
    updating: Boolean,
    isError: Boolean = false,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        enabled = !checking && !updating,
        modifier = modifier.height(112.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun UpdateSourceSettings(viewModel: UpdateViewModel) {
    val updateSource by viewModel.updateSource.collectAsStateWithLifecycle()
    val mirrorChyanCdk by viewModel.mirrorChyanCdk.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showInfoSource by remember { mutableStateOf<UpdateSource?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        SettingDropdown(
            title = stringResource(R.string.update_card_source_label),
            selected = updateSource,
            options = UpdateSource.entries,
            optionLabel = { stringResource(it.resId) },
            onSelected = viewModel::setUpdateSource,
            icon = Icons.Rounded.CloudDownload,
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (updateSource != UpdateSource.GITHUB) {
                TextButton(onClick = { showInfoSource = updateSource }) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.update_card_about_source_cd, stringResource(updateSource.resId)))
                }
            }
            AnimatedVisibility(visible = updateSource == UpdateSource.MIRROR_CHYAN) {
                CdkInputField(
                    cdk = mirrorChyanCdk,
                    onCdkChange = viewModel::setMirrorChyanCdk,
                )
            }
        }
    }

    showInfoSource?.let { source ->
        val sourceName = stringResource(source.resId)
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.update_card_about_title, sourceName),
            onConfirm = {
                Misc.openUriSafely(
                    context = context,
                    uriString = when (source) {
                        UpdateSource.GITHUB -> "https://github.com/MaaAssistantArknights/MaaResource"
                        UpdateSource.MIRROR_CHYAN -> "https://mirrorchyan.com/zh/projects?rid=MAA&os=android&channel=stable&source=maameow"
                    },
                )
                showInfoSource = null
            },
            onDismissRequest = { showInfoSource = null },
            confirmText = stringResource(R.string.update_card_visit_site),
            dismissText = stringResource(R.string.common_close),
            icon = Icons.Rounded.Info,
            landscapeAdaptive = true,
            content = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when (source) {
                        UpdateSource.GITHUB -> Text(
                            text = stringResource(R.string.update_card_github_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        UpdateSource.MIRROR_CHYAN -> {
                            val mirrorBrand = stringResource(R.string.update_card_mirror_brand)
                            val mirrorDesc = stringResource(R.string.update_card_mirror_desc)
                            val primary = MaterialTheme.colorScheme.primary
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = primary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    ) {
                                        append(mirrorBrand)
                                    }
                                    append(" ")
                                    append(mirrorDesc)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            },
        )
    }
}

/**
 * CDK 输入框
 */
@Composable
private fun CdkInputField(
    cdk: String,
    onCdkChange: (String) -> Unit
) {
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    var localCdk by remember { mutableStateOf(cdk) }
    LaunchedEffect(cdk) {
        if (cdk != localCdk) {
            localCdk = cdk
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = localCdk,
            onValueChange = { newValue ->
                localCdk = newValue
                onCdkChange(newValue)
            },
            label = { Text(stringResource(R.string.update_cdk_label)) },
            placeholder = { Text(stringResource(R.string.update_cdk_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row {
                    if (localCdk.isNotEmpty()) {
                        IconButton(onClick = {
                            localCdk = ""
                            onCdkChange("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.update_cdk_clear_cd)
                            )
                        }
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.Lock else Icons.Filled.Lock,
                            contentDescription = if (passwordVisible)
                                stringResource(R.string.update_cdk_hide_cd)
                            else
                                stringResource(R.string.update_cdk_show_cd)
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
            onClick = { Misc.openUriSafely(context, "https://mirrorchyan.com/") }
        ) {
            Text(
                text = stringResource(R.string.update_cdk_subscribe),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


@Composable
private fun AppUpdateProgress(appUpdateState: UpdateProcessState) {
    when (appUpdateState) {
        is UpdateProcessState.Downloading -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.update_progress_app_downloading, appUpdateState.progress.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = appUpdateState.speed,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
                LinearProgressIndicator(
                    progress = { appUpdateState.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        is UpdateProcessState.Installing -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(R.string.update_progress_app_installing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        else -> {}
    }
}

/**
 * 资源更新进度显示
 */
@Composable
private fun ResourceUpdateProgress(resourceUpdateState: UpdateProcessState) {
    when (resourceUpdateState) {
        is UpdateProcessState.Downloading -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.update_progress_resource_downloading, resourceUpdateState.progress.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = resourceUpdateState.speed,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
                LinearProgressIndicator(
                    progress = { resourceUpdateState.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        is UpdateProcessState.Extracting -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.update_progress_resource_extracting, resourceUpdateState.progress.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "${resourceUpdateState.current}/${resourceUpdateState.total}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
                LinearProgressIndicator(
                    progress = { resourceUpdateState.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        else -> {}
    }
}

/**
 * 更新源选择按钮组
 */
/**
 * 应用更新确认弹窗
 */
@Composable
private fun AppUpdateConfirmDialog(
    updateInfo: UpdateInfo,
    currentVersion: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AdaptiveTaskPromptDialog(
        visible = true,
        title = stringResource(R.string.dialog_update_found_title),
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.dialog_update_now),
        confirmColor = Color(0xFF4CAF50),
        dismissText = stringResource(R.string.dialog_update_later),
        icon = Icons.Rounded.Info,
        landscapeAdaptive = true,
        content = {
            Column {
                Text(
                    text = "$currentVersion → ${updateInfo.version}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                if (!updateInfo.releaseNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MarkdownText(
                        markdown = updateInfo.releaseNote,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    )
}

/**
 * 错误提示弹窗
 */
@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AdaptiveTaskPromptDialog(
        visible = true,
        title = stringResource(R.string.dialog_update_failed_title),
        message = message,
        onConfirm = onDismiss,
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.common_confirm),
        dismissText = null,
        icon = Icons.Rounded.Warning,
        confirmColor = MaterialTheme.colorScheme.error
    )
}
