package com.aliothmoon.maameow.presentation.view.panel

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.presentation.navigation.LocalMainBottomBarPadding
import com.aliothmoon.maameow.data.resource.CopilotResourceProvider
import com.aliothmoon.maameow.domain.service.OperatorDisplayItem
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.presentation.LocalFloatingWindowContext
import com.aliothmoon.maameow.presentation.components.ExpressiveSwitch
import com.aliothmoon.maameow.presentation.components.ITextField
import com.aliothmoon.maameow.presentation.components.LocalSettingItemShape
import com.aliothmoon.maameow.presentation.components.NumberStepperSettingRow
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingDropdown
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.viewmodel.CopilotViewModel
import com.aliothmoon.maameow.utils.Misc
import com.aliothmoon.maameow.utils.i18n.asString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class CopilotTabUiSpec(
    val index: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int? = null,
    val supportsBattleList: Boolean,
    val supportsRegularOptions: Boolean,
)

@Composable
fun AutoBattlePanel(
    modifier: Modifier = Modifier,
    viewModel: CopilotViewModel = koinInject()
) {
    val mainBottomBarPadding = LocalMainBottomBarPadding.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val maaState by viewModel.maaState.collectAsStateWithLifecycle()
    val isStarting = maaState == MaaExecutionState.STARTING
    val controlsEnabled = !state.isLoading && !isStarting
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isInFloatingWindow = LocalFloatingWindowContext.current
    val statusMessage = state.statusMessage.asString()
    val compactButtonShape = RoundedCornerShape(12.dp)
    val compactButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    val importFloatHint = stringResource(R.string.copilot_import_float_hint)

    // SAF 文件选择器（浮窗环境下不可用）
    val filePicker = if (!isInFloatingWindow) {
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            scope.launch {
                val files = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        val name = Misc.queryFileName(context, uri)
                            ?: uri.lastPathSegment
                            ?: "copilot_${System.currentTimeMillis()}.json"
                        val json = context.contentResolver.openInputStream(uri)?.use {
                            it.bufferedReader().readText()
                        } ?: return@mapNotNull null
                        name to json
                    }
                }
                if (files.isNotEmpty()) {
                    viewModel.onImportLocalFiles(files)
                }
            }
        }
    } else null
    val tabSpecs = listOf(
        CopilotTabUiSpec(
            index = 0,
            titleRes = R.string.panel_autobattle_tab_mainline,
            subtitleRes = R.string.panel_autobattle_tab_mainline_subtitle,
            supportsBattleList = true,
            supportsRegularOptions = true,
        ),
        CopilotTabUiSpec(
            index = 1,
            titleRes = R.string.panel_autobattle_tab_security,
            supportsBattleList = false,
            supportsRegularOptions = false,
        ),
        CopilotTabUiSpec(
            index = 2,
            titleRes = R.string.panel_autobattle_tab_paradox,
            supportsBattleList = true,
            supportsRegularOptions = false,
        ),
        CopilotTabUiSpec(
            index = 3,
            titleRes = R.string.panel_autobattle_tab_other,
            supportsBattleList = false,
            supportsRegularOptions = true,
        )
    )
    val tabLabels = listOf(
        stringResource(R.string.panel_autobattle_tab_mainline) + " / " +
            stringResource(R.string.panel_autobattle_tab_mainline_subtitle),
        stringResource(R.string.panel_autobattle_tab_security),
        stringResource(R.string.panel_autobattle_tab_paradox),
        stringResource(R.string.panel_autobattle_tab_other),
    )
    val current = tabSpecs.firstOrNull { it.index == state.tabIndex } ?: tabSpecs.first()
    val regularCopilotTab = current.supportsRegularOptions
    val loopCountSupportedTab = current.index == 1 || current.index == 3
    val battleListSupportedTab = current.supportsBattleList


    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp),
            contentPadding = PaddingValues(bottom = mainBottomBarPadding + 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SegmentedSettingsGroup {
                    item {
                        SettingDropdown(
                            title = stringResource(R.string.panel_autobattle_job_type),
                            selected = current,
                            options = tabSpecs,
                            optionLabel = { spec -> tabLabels[spec.index] },
                            onSelected = { viewModel.onTabChanged(it.index) },
                            icon = null,
                            singleLine = true,
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        CopilotActionButton(
                            text = if (state.isLoading) {
                                stringResource(R.string.panel_autobattle_loading)
                            } else {
                                stringResource(R.string.panel_autobattle_read_single)
                            },
                            icon = Icons.Default.Search,
                            filled = true,
                            enabled = controlsEnabled,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 5.dp,
                                bottomEnd = 5.dp,
                            ),
                            onClick = viewModel::onParseInput,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        CopilotActionButton(
                            text = stringResource(R.string.copilot_import_file),
                            icon = Icons.Default.UploadFile,
                            filled = false,
                            enabled = controlsEnabled,
                            shape = RoundedCornerShape(
                                topStart = 5.dp,
                                topEnd = 5.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 5.dp,
                            ),
                            onClick = {
                                if (filePicker != null) {
                                    filePicker.launch(
                                        arrayOf("application/json", "application/octet-stream"),
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        importFloatHint,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                        CopilotActionButton(
                            text = stringResource(R.string.panel_autobattle_station),
                            icon = Icons.Default.Public,
                            filled = false,
                            enabled = true,
                            shape = RoundedCornerShape(
                                topStart = 5.dp,
                                topEnd = 5.dp,
                                bottomStart = 5.dp,
                                bottomEnd = 16.dp,
                            ),
                            onClick = { Misc.openUriSafely(context, "https://zoot.plus") },
                        )
                    }
                }
            }

            item {
                SegmentedSettingsGroup {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ITextField(
                                value = state.inputText,
                                onValueChange = viewModel::onInputChanged,
                                label = stringResource(R.string.panel_autobattle_station_code_label),
                                placeholder = stringResource(
                                    R.string.panel_autobattle_station_code_placeholder,
                                ),
                                shape = RoundedCornerShape(16.dp),
                                trailingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.offset(x = (-4).dp),
                                    ) {
                                        IconButton(
                                            onClick = viewModel::onPasteAndParse,
                                            enabled = controlsEnabled,
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = stringResource(
                                                    R.string.copilot_paste_parse,
                                                ),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                        IconButton(
                                            onClick = viewModel::onToggleBuiltinPicker,
                                            enabled = controlsEnabled,
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (state.builtinPickerExpanded) {
                                                    Icons.Default.ExpandLess
                                                } else {
                                                    Icons.Default.ExpandMore
                                                },
                                                contentDescription = stringResource(
                                                    R.string.copilot_builtin_picker,
                                                ),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                    if (state.builtinPickerExpanded) {
                        item {
                            BuiltinCopilotTree(
                                loaded = state.builtinLoaded,
                                tree = state.builtinTree,
                                expandedFolders = state.builtinExpandedFolders,
                                enabled = controlsEnabled,
                                onToggleFolder = viewModel::onToggleBuiltinFolder,
                                onSelectFile = viewModel::onSelectBuiltinFile,
                            )
                        }
                    }
                    item {
                        SettingRow(
                            title = statusMessage.ifBlank {
                                stringResource(R.string.panel_autobattle_waiting)
                            },
                            icon = null,
                            trailing = if (state.isLoading) {
                                {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            } else null,
                        )
                    }
                }
            }

            // 作业详情 + 视频链接
            if (state.currentCopilot != null) {
                val doc = state.currentCopilot!!.doc
                val hasDetail =
                    doc.title.isNotBlank() || doc.details.isNotBlank() || state.operatorSummary?.isEmpty == false
                val hasVideo = state.videoUrl.isNotBlank()
                if (hasDetail || hasVideo) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceBright,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SelectionContainer {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (doc.title.isNotBlank()) {
                                            Text(
                                                text = doc.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (doc.details.isNotBlank()) {
                                            Text(
                                                text = doc.details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                val summary = state.operatorSummary
                                if (summary != null && !summary.isEmpty) {
                                    if (doc.title.isNotBlank() || doc.details.isNotBlank()) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.2f
                                            )
                                        )
                                    }
                                    val textMeasurer = rememberTextMeasurer()
                                    val labelStyle = MaterialTheme.typography.labelSmall
                                    val density = LocalDensity.current
                                    val nameColumnWidth = remember(summary) {
                                        val allNames = summary.operators.map { it.name } +
                                                summary.groups.flatMap { (_, opers) -> opers.map { it.name } }
                                        val maxTextWidth = allNames.maxOfOrNull { name ->
                                            textMeasurer.measure(name, labelStyle).size.width
                                        } ?: 0
                                        maxTextWidth + with(density) { 8.dp.roundToPx() }
                                    }
                                    val nameWidth = remember(nameColumnWidth) {
                                        with(density) { nameColumnWidth.toDp() }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // 独立干员
                                        if (summary.operators.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = stringResource(R.string.panel_autobattle_operator_header),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                summary.operators.forEach { oper ->
                                                    OperatorRow(oper, nameWidth = nameWidth)
                                                }
                                            }
                                        }
                                        // 备选组
                                        summary.groups.forEach { (groupName, opers) ->
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.panel_autobattle_group_header,
                                                        groupName
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                opers.forEach { oper ->
                                                    OperatorRow(oper, nameWidth = nameWidth)
                                                }
                                            }
                                        }
                                        // 统计
                                        Text(
                                            text = stringResource(
                                                R.string.panel_autobattle_summary_count,
                                                summary.totalCount
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.6f
                                            )
                                        )
                                    }
                                }
                                if (hasVideo) {
                                    Text(
                                        text = stringResource(R.string.common_video),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable {
                                            Misc.openUriSafely(
                                                context,
                                                state.videoUrl
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (regularCopilotTab) {
                item {
                    TaskSettingsSectionTitle(stringResource(R.string.panel_autobattle_settings))
                }
                item {
                    SegmentedSettingsGroup {
                        item {
                            CopilotSwitchRow(
                                title = stringResource(R.string.panel_autobattle_auto_formation),
                                checked = state.config.formation,
                                onCheckedChange = {
                                    viewModel.onConfigChanged(state.config.copy(formation = it))
                                },
                            )
                        }
                        if (state.config.formation) {
                            item {
                                CopilotSwitchRow(
                                    title = stringResource(R.string.panel_autobattle_use_formation),
                                    checked = state.config.useFormation,
                                    onCheckedChange = { enabled ->
                                        viewModel.onConfigChanged(
                                            state.config.copy(
                                                useFormation = enabled,
                                                formationIndex = state.config.formationIndex.coerceIn(1, 4),
                                            ),
                                        )
                                    },
                                )
                            }
                            if (state.config.useFormation) {
                                item {
                                    SettingDropdown(
                                        title = stringResource(R.string.panel_autobattle_formation),
                                        selected = state.config.formationIndex,
                                        options = listOf(1, 2, 3, 4),
                                        optionLabel = { it.toString() },
                                        onSelected = {
                                            viewModel.onConfigChanged(
                                                state.config.copy(formationIndex = it),
                                            )
                                        },
                                        icon = null,
                                        singleLine = true,
                                    )
                                }
                            }
                            item {
                                CopilotSwitchRow(
                                    title = stringResource(
                                        R.string.panel_autobattle_ignore_requirements,
                                    ),
                                    checked = state.config.ignoreRequirements,
                                    onCheckedChange = {
                                        viewModel.onConfigChanged(
                                            state.config.copy(ignoreRequirements = it),
                                        )
                                    },
                                )
                            }
                            item {
                                CopilotSwitchRow(
                                    title = stringResource(R.string.panel_autobattle_support_unit),
                                    checked = state.config.useSupportUnit,
                                    onCheckedChange = {
                                        viewModel.onConfigChanged(
                                            state.config.copy(useSupportUnit = it),
                                        )
                                    },
                                )
                            }
                            if (state.config.useSupportUnit) {
                                item {
                                    val supportLabels = mapOf(
                                        1 to stringResource(
                                            R.string.panel_autobattle_support_fill_gaps,
                                        ),
                                        3 to stringResource(R.string.panel_autobattle_support_random),
                                    )
                                    SettingDropdown(
                                        title = stringResource(
                                            R.string.panel_autobattle_support_strategy,
                                        ),
                                        selected = state.config.supportUnitUsage,
                                        options = listOf(1, 3),
                                        optionLabel = { supportLabels[it].orEmpty() },
                                        onSelected = {
                                            viewModel.onConfigChanged(
                                                state.config.copy(supportUnitUsage = it),
                                            )
                                        },
                                        icon = null,
                                        singleLine = true,
                                    )
                                }
                            }
                            item {
                                CopilotSwitchRow(
                                    title = stringResource(R.string.panel_autobattle_add_trust),
                                    checked = state.config.addTrust,
                                    onCheckedChange = {
                                        viewModel.onConfigChanged(state.config.copy(addTrust = it))
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                TaskSettingsSectionTitle(stringResource(R.string.panel_autobattle_execution))
            }

            item {
                SegmentedSettingsGroup {
                    if (battleListSupportedTab) {
                        item {
                            CopilotSwitchRow(
                                title = stringResource(R.string.panel_autobattle_battle_list),
                                checked = state.useCopilotList,
                                onCheckedChange = viewModel::onToggleListMode,
                            )
                        }
                    }

                    if (state.useCopilotList && state.tabIndex == 0) {
                        item {
                            CopilotSwitchRow(
                                title = stringResource(
                                    R.string.panel_autobattle_use_sanity_potion,
                                ),
                                checked = state.config.useSanityPotion,
                                onCheckedChange = {
                                    viewModel.onConfigChanged(
                                        state.config.copy(useSanityPotion = it),
                                    )
                                },
                            )
                        }
                    }

                    if (!state.useCopilotList && loopCountSupportedTab) {
                        item {
                            CopilotSwitchRow(
                                title = stringResource(R.string.panel_autobattle_loop),
                                checked = state.config.loop,
                                onCheckedChange = {
                                    viewModel.onConfigChanged(state.config.copy(loop = it))
                                },
                            )
                        }
                        if (state.config.loop) {
                            item {
                                NumberStepperSettingRow(
                                    title = stringResource(R.string.panel_autobattle_loop_count),
                                    value = state.config.loopTimes.coerceAtLeast(1),
                                    range = 1..999,
                                    icon = null,
                                    onValueChange = {
                                        viewModel.onConfigChanged(
                                            state.config.copy(loopTimes = it),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }


            item {
                if (state.useCopilotList && battleListSupportedTab) {
                    TaskSettingsSectionTitle(
                        stringResource(R.string.panel_autobattle_battle_list),
                    )
                    if (state.taskList.isEmpty()) {
                        SegmentedSettingsGroup {
                            item {
                                SettingRow(
                                    title = stringResource(
                                        R.string.panel_autobattle_empty_entries,
                                    ),
                                    icon = null,
                                    enabled = false,
                                )
                            }
                        }
                    } else {
                        val lazyListState = rememberLazyListState()
                        val reorderableState = rememberReorderableLazyListState(
                            lazyListState = lazyListState,
                            onMove = { from, to ->
                                viewModel.onReorderList(from.index, to.index)
                            },
                        )
                        LazyColumn(
                            state = lazyListState,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                        ) {
                            itemsIndexed(
                                state.taskList,
                                key = { index, item ->
                                    val duplicateIndex = state.taskList
                                        .take(index)
                                        .count {
                                            it.filePath == item.filePath &&
                                                it.name == item.name &&
                                                it.isRaid == item.isRaid
                                        }
                                    "${item.filePath}|${item.name}|${item.isRaid}|$duplicateIndex"
                                },
                            ) { index, item ->
                                val duplicateIndex = state.taskList
                                    .take(index)
                                    .count {
                                        it.filePath == item.filePath &&
                                            it.name == item.name &&
                                            it.isRaid == item.isRaid
                                    }
                                val itemKey =
                                    "${item.filePath}|${item.name}|${item.isRaid}|$duplicateIndex"
                                ReorderableItem(
                                    reorderableState,
                                    key = itemKey,
                                ) { isDragging ->
                                    val itemShape = when {
                                        state.taskList.size == 1 -> RoundedCornerShape(16.dp)
                                        index == 0 -> RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = 5.dp,
                                            bottomEnd = 5.dp,
                                        )
                                        index == state.taskList.lastIndex -> RoundedCornerShape(
                                            topStart = 5.dp,
                                            topEnd = 5.dp,
                                            bottomStart = 16.dp,
                                            bottomEnd = 16.dp,
                                        )
                                        else -> RoundedCornerShape(5.dp)
                                    }
                                    Surface(
                                        onClick = { viewModel.onToggleListItem(index) },
                                        tonalElevation = if (isDragging) 2.dp else 0.dp,
                                        shape = itemShape,
                                        color = MaterialTheme.colorScheme.surfaceBright,
                                        modifier = Modifier
                                            .longPressDraggableHandle()
                                            .fillMaxWidth(),
                                    ) {
                                        CompositionLocalProvider(
                                            LocalSettingItemShape provides itemShape,
                                        ) {
                                            SettingRow(
                                                title = item.name + if (item.isRaid) {
                                                    stringResource(
                                                        R.string.panel_autobattle_raid_suffix,
                                                    )
                                                } else {
                                                    ""
                                                },
                                                icon = null,
                                                trailing = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Icon(
                                                            imageVector = if (item.isChecked) {
                                                                Icons.Outlined.CheckCircle
                                                            } else {
                                                                Icons.Outlined.RadioButtonUnchecked
                                                            },
                                                            contentDescription = null,
                                                            tint = if (item.isChecked) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.onSurfaceVariant
                                                            },
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.onSelectListItem(index)
                                                            },
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Search,
                                                                contentDescription = stringResource(
                                                                    R.string.common_load,
                                                                ),
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.onRemoveFromList(index)
                                                            },
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = stringResource(
                                                                    R.string.common_delete,
                                                                ),
                                                                tint = MaterialTheme.colorScheme.error,
                                                            )
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = viewModel::onCleanUnchecked,
                            enabled = state.taskList.any { !it.isChecked },
                            shape = compactButtonShape,
                            contentPadding = compactButtonPadding,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text(stringResource(R.string.panel_autobattle_clear_unchecked)) }
                        Button(
                            onClick = viewModel::onClearList,
                            enabled = state.taskList.isNotEmpty(),
                            shape = compactButtonShape,
                            contentPadding = compactButtonPadding,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text(stringResource(R.string.panel_autobattle_clear_list)) }
                    }
                }
            }




            item {
                var expanded by remember { mutableStateOf(true) }
                Surface(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.panel_autobattle_tips_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = if (expanded) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Text(
                                text = stringResource(R.string.panel_autobattle_tips_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 14.dp,
                                ),
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun OperatorRow(
    item: OperatorDisplayItem,
    nameWidth: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .width(nameWidth)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                textAlign = TextAlign.Start
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            item.tags.forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * 自动战斗操作按钮：等宽网格单元（RowScope.weight(1f)），图标 + 单行文字。
 * @param filled true=实心主色（读取类主操作）；false=描边次级（导入/外链）
 */
@Composable
private fun RowScope.CopilotActionButton(
    text: String,
    icon: ImageVector,
    filled: Boolean,
    enabled: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    val padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    val content: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (filled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            modifier = Modifier.weight(1f).height(48.dp),
            content = content,
        )
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            modifier = Modifier.weight(1f).height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            content = content,
        )
    }
}

private data class BuiltinVisibleEntry(
    val node: CopilotResourceProvider.Node,
    val depth: Int,
)

@Composable
private fun CopilotSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingRow(
        title = title,
        icon = null,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

@Composable
private fun BuiltinCopilotTree(
    loaded: Boolean,
    tree: List<CopilotResourceProvider.Node>,
    expandedFolders: Set<String>,
    enabled: Boolean,
    onToggleFolder: (String) -> Unit,
    onSelectFile: (CopilotResourceProvider.Node) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleNodes = remember(tree, expandedFolders) {
        flattenVisibleNodes(tree, expandedFolders)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .heightIn(max = 320.dp)
            .animateContentSize()
    ) {
        when {
            !loaded -> CircularProgressIndicator(
                modifier = Modifier
                    .padding(12.dp)
                    .size(16.dp),
                strokeWidth = 2.dp
            )

            visibleNodes.isEmpty() -> Text(
                text = stringResource(R.string.copilot_builtin_picker_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(visibleNodes, key = { it.node.relativePath }) { entry ->
                    BuiltinNodeRow(
                        entry = entry,
                        expanded = entry.node.relativePath in expandedFolders,
                        enabled = enabled,
                        onClick = {
                            if (entry.node.isFolder) {
                                onToggleFolder(entry.node.relativePath)
                            } else {
                                onSelectFile(entry.node)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun flattenVisibleNodes(
    tree: List<CopilotResourceProvider.Node>,
    expandedFolders: Set<String>,
    depth: Int = 0,
    out: MutableList<BuiltinVisibleEntry> = mutableListOf(),
): List<BuiltinVisibleEntry> {
    for (node in tree) {
        out.add(BuiltinVisibleEntry(node, depth))
        if (node.isFolder && node.relativePath in expandedFolders) {
            flattenVisibleNodes(node.children, expandedFolders, depth + 1, out)
        }
    }
    return out
}

@Composable
private fun BuiltinNodeRow(
    entry: BuiltinVisibleEntry,
    expanded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val node = entry.node
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                start = (8 + entry.depth * 16).dp,
                end = 8.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (node.isFolder) {
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(18.dp))
        }
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (node.isFolder) FontWeight.Medium else FontWeight.Normal,
            color = if (node.isFolder) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
