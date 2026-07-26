package com.aliothmoon.maameow.schedule.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.schedule.model.ExecutionResult
import com.aliothmoon.maameow.schedule.model.ScheduleStrategy
import com.aliothmoon.maameow.schedule.service.AutoStartHelper
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduleListView(
    navController: NavController,
    viewModel: ScheduleListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }
    var showAutoStartGuide by remember { mutableStateOf(false) }

    // 首次有策略时检查是否需要自启动引导
    LaunchedEffect(state.strategies.isNotEmpty()) {
        if (state.strategies.isNotEmpty() && AutoStartHelper.isKnownRestrictiveManufacturer()) {
            val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("autostart_guided", false)) {
                val intent = AutoStartHelper.getAutoStartIntent(context)
                if (intent != null) {
                    showAutoStartGuide = true
                    prefs.edit { putBoolean("autostart_guided", true) }
                }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.schedule_title),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SCHEDULE_TRIGGER_LOG) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.schedule_trigger_log_title)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("schedule_edit/new") }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.schedule_create_strategy))
            }
        },
        // 外层 MainScreen 的 bottomBar 已消费导航栏 inset；此处不再重复预留，
        // 否则底部会多出一条等高于导航栏的空白条
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (state.strategies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.schedule_empty_state),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.schedule_empty_hint_add),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Adaptive(minSize = 320.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.strategies, key = { it.id }) { strategy ->
                    val profileName = state.profiles.find { it.id == strategy.profileId }?.name
                    StrategyCard(
                        strategy = strategy,
                        profileName = profileName,
                        nextTrigger = viewModel.getNextTriggerTime(strategy),
                        onToggleEnabled = { viewModel.onToggleEnabled(strategy.id, it) },
                        onClick = { navController.navigate("schedule_edit/${strategy.id}") },
                        onDelete = { deleteConfirmId = strategy.id }
                    )
                }
            }
        }

        if (deleteConfirmId != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmId = null },
                title = { Text(stringResource(R.string.schedule_delete_strategy_title)) },
                text = { Text(stringResource(R.string.schedule_delete_strategy_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onDeleteStrategy(deleteConfirmId!!)
                        deleteConfirmId = null
                    }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmId = null }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

        if (showAutoStartGuide) {
            AlertDialog(
                onDismissRequest = { showAutoStartGuide = false },
                title = { Text(stringResource(R.string.schedule_auto_start_permission_title)) },
                text = { Text(stringResource(R.string.schedule_auto_start_permission_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        AutoStartHelper.getAutoStartIntent(context)?.let {
                            runCatching { context.startActivity(it) }
                        }
                        showAutoStartGuide = false
                    }) { Text(stringResource(R.string.schedule_go_to_settings)) }
                },
                dismissButton = {
                    TextButton(onClick = { showAutoStartGuide = false }) { Text(stringResource(R.string.schedule_later)) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StrategyCard(
    strategy: ScheduleStrategy,
    profileName: String?,
    nextTrigger: String?,
    onToggleEnabled: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strategy.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (strategy.enabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (strategy.enabled) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        text = stringResource(
                            if (strategy.enabled) R.string.schedule_status_enabled
                            else R.string.schedule_status_disabled,
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text(
                text = localizedScheduleStrategySummary(strategy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (profileName != null) {
                Text(
                    text = profileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (strategy.enabled && nextTrigger != null) {
                Text(
                    text = stringResource(R.string.schedule_next_trigger, nextTrigger),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val lastResultText = strategy.lastResult?.let {
                formatExecutionResult(it, strategy.lastResultMessage)
            }
            if (lastResultText != null) {
                Text(
                    text = lastResultText,
                    style = MaterialTheme.typography.labelMedium,
                    color = executionResultColor(
                        strategy.lastResult,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.schedule_edit_title_edit),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Switch(
                checked = strategy.enabled,
                onCheckedChange = onToggleEnabled,
            )
        }
    }
}

@Composable
private fun formatExecutionResult(result: ExecutionResult, message: String?): String {
    val label = when (result) {
        ExecutionResult.STARTED,
        ExecutionResult.FAILED_VALIDATION,
        ExecutionResult.FAILED_START,
        ExecutionResult.FAILED_UI_LAUNCH,
        ExecutionResult.SKIPPED_BUSY,
        ExecutionResult.CANCELLED -> {
            stringResource(R.string.schedule_last_result, scheduleExecutionResultLabel(result))
        }

        else -> return ""
    }
    return if (message.isNullOrBlank()) label else "$label · $message"
}

private fun executionResultColor(
    result: ExecutionResult?,
    successColor: Color,
    errorColor: Color,
    warningColor: Color,
): Color {
    return when (result) {
        ExecutionResult.STARTED -> successColor
        ExecutionResult.SKIPPED_BUSY,
        ExecutionResult.CANCELLED -> warningColor

        ExecutionResult.FAILED_VALIDATION,
        ExecutionResult.FAILED_START,
        ExecutionResult.FAILED_UI_LAUNCH -> errorColor

        else -> Color.Unspecified
    }
}
