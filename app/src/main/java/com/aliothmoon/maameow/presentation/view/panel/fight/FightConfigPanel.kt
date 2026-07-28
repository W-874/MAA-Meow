package com.aliothmoon.maameow.presentation.view.panel.fight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.model.StageResetMode
import com.aliothmoon.maameow.data.resource.ActivityManager
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.StageAliasMapper
import com.aliothmoon.maameow.data.resource.StageGroup
import com.aliothmoon.maameow.domain.enums.UiUsageConstants
import com.aliothmoon.maameow.domain.models.SeriesLock
import com.aliothmoon.maameow.presentation.components.CheckBoxWithExpandableTip
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.view.panel.TaskSettingsSectionTitle
import com.aliothmoon.maameow.presentation.view.panel.LocalTaskPanelBottomPadding
import com.aliothmoon.maameow.presentation.components.ITextFieldWithFocus
import com.aliothmoon.maameow.presentation.components.SelectableChipGroup
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingDropdown
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.components.SettingActionButton
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipContent
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipIcon
import com.aliothmoon.maameow.presentation.view.panel.common.StageInputField
import com.aliothmoon.maameow.presentation.view.panel.common.StageOptionGroups
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


private val MEDICINE_EXPIRE_DAY_OPTIONS = listOf(
    1 to "24h x 1", 2 to "24h x 2", 3 to "24h x 3", 4 to "24h x 4",
    5 to "24h x 5", 6 to "24h x 6", 7 to "24h x 7"
)

@Composable
fun FightConfigPanel(
    config: FightConfig,
    clientType: String,
    onConfigChange: (FightConfig) -> Unit,
    modifier: Modifier = Modifier,
    activityManager: ActivityManager = koinInject(),
    itemHelper: ItemHelper = koinInject()
) {
    // 资源收集
    val resourceCollectionInfo by activityManager.resourceCollection.collectAsStateWithLifecycle()
    val isResourceCollectionOpen = resourceCollectionInfo?.isOpen == true

    val dropItemsList by itemHelper.dropItems.collectAsStateWithLifecycle()
    val activityStages by activityManager.activityStages.collectAsStateWithLifecycle()
    val stageTips = remember(activityStages) { activityManager.getStageTips() }
    val todayName = remember(activityStages) { activityManager.getYjDayOfWeekName() }

    // 分组列表 -- 依赖 hideUnavailableStage 和活动关卡数据
    val stageGroups = remember(activityStages, config.hideUnavailableStage) {
        activityManager.getMergedStageGroups(config.hideUnavailableStage)
    }


    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            bottom = LocalTaskPanelBottomPadding.current,
        ),
    ) {
        item {
            TodayStagesHint(
                stageGroups = stageGroups,
                isResourceCollectionOpen = isResourceCollectionOpen,
                stageTips = stageTips,
                todayName = todayName,
            )
        }
        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_general)) }
                        item {
                            MedicineAndStoneSection(config, onConfigChange)
                        }
                        item {
                            SpecifiedDropsSection(
                                config,
                                onConfigChange,
                                dropItemsList,
                            )
                        }
                        // 代理倍率（HideSeries=false 时显示）
                        if (!config.hideSeries) {
                            item {
                                SeriesSection(config, clientType, onConfigChange)
                            }
                        }
                        item {
                            GroupedStageSelectionSection(
                                config = config,
                                onConfigChange = onConfigChange,
                                stageGroups = stageGroups,
                                activityManager = activityManager,
                            )
                        }
        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_advanced)) }
                        item {
                            SegmentedSettingsGroup {
                        item { CustomAnnihilationSection(config, onConfigChange) }
                        item {
                            // 博朗台模式
                            CheckBoxWithExpandableTip(
                                checked = config.isDrGrandet,
                                onCheckedChange = { onConfigChange(config.copy(isDrGrandet = it)) },
                                label = stringResource(R.string.panel_fight_dr_grandet),
                                tipText = stringResource(R.string.panel_fight_dr_grandet_tip)
                            )
                        }
                        item {
                            // 自定义关卡代码
                            CheckBoxWithExpandableTip(
                                checked = config.customStageCode,
                                onCheckedChange = { onConfigChange(config.copy(customStageCode = it)) },
                                label = stringResource(R.string.panel_fight_custom_stage_code),
                                tipText = stringResource(R.string.panel_fight_custom_stage_code_tip)
                            )
                        }
                        item {
                            // 使用备选关卡
                            CheckBoxWithExpandableTip(
                                checked = config.useAlternateStage,
                                onCheckedChange = {
                                    onConfigChange(
                                        config.copy(
                                            useAlternateStage = it,
                                            // 启用备选关卡时，自动禁用隐藏不可用关卡，重置策略设为 IGNORE
                                            hideUnavailableStage = if (it) false else config.hideUnavailableStage,
                                            stageResetMode = if (it) StageResetMode.IGNORE else config.stageResetMode
                                        )
                                    )
                                },
                                label = stringResource(R.string.panel_fight_use_alternate_stage),
                                tipText = stringResource(R.string.panel_fight_use_alternate_stage_tip)
                            )
                        }
                        // TODO 暂时关闭 源石使用
//                        item {
//                            // 允许保存源石使用
//                            AllowUseStoneSaveSection(config, onConfigChange)
//                        }
                        item {
                            Column {
                                // 使用即将过期的理智药
                                CheckBoxWithExpandableTip(
                                    checked = config.useExpiringMedicine,
                                    onCheckedChange = { onConfigChange(config.copy(useExpiringMedicine = it)) },
                                    label = stringResource(R.string.panel_fight_use_expiring_medicine),
                                    tipText = stringResource(R.string.panel_fight_use_expiring_medicine_tip)
                                )
                                AnimatedVisibility(
                                    visible = config.useExpiringMedicine,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SettingDropdown(
                                            title = stringResource(R.string.panel_fight_medicine_expire_days),
                                            selected = config.medicineExpireDays,
                                            options = MEDICINE_EXPIRE_DAY_OPTIONS.map { it.first },
                                            optionLabel = { value ->
                                                MEDICINE_EXPIRE_DAY_OPTIONS.firstOrNull { it.first == value }?.second.orEmpty()
                                            },
                                            onSelected = { onConfigChange(config.copy(medicineExpireDays = it)) },
                                            icon = null,
                                        )
                                        CheckBoxWithExpandableTip(
                                            checked = config.useExpireMedicineForActivity,
                                            onCheckedChange = { onConfigChange(config.copy(useExpireMedicineForActivity = it)) },
                                            label = stringResource(R.string.panel_fight_use_expire_medicine_for_activity),
                                            tipText = stringResource(R.string.panel_fight_use_expire_medicine_for_activity_tip)
                                        )
                                        val summaries = remember { activityManager.getOpenActivitySummaries() }
                                        val daysLeftLabel = stringResource(R.string.panel_fight_activity_days_left_open)
                                        val lessThanOneDay = stringResource(R.string.panel_fight_activity_less_than_one_day)
                                        if (summaries.isNotEmpty()) {
                                            summaries.forEach { s ->
                                                val dayText = if (s.daysLeft > 0) "${s.daysLeft}+" else lessThanOneDay
                                                Text(
                                                    text = "｢${s.name}｣ $daysLeftLabel$dayText",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (s.isExpiringSoon) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = stringResource(R.string.panel_fight_no_activity),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            // 隐藏不可用关卡
                            CheckBoxWithExpandableTip(
                                checked = config.hideUnavailableStage,
                                onCheckedChange = {
                                    onConfigChange(
                                        config.copy(
                                            hideUnavailableStage = it,
                                            // 启用隐藏不可用关卡时，自动禁用使用备选关卡，重置策略设为 CURRENT
                                            useAlternateStage = if (it) false else config.useAlternateStage,
                                            stageResetMode = if (it) StageResetMode.CURRENT else config.stageResetMode
                                        )
                                    )
                                },
                                label = stringResource(R.string.panel_fight_hide_unavailable_stage),
                                tipText = stringResource(R.string.panel_fight_hide_unavailable_stage_tip)
                            )
                        }
                        item {
                            // 未开放关卡重置策略
                            StageResetModeSection(config, onConfigChange)
                        }
                        item {
                            // 隐藏代理倍率
                            CheckBoxWithLabel(
                                checked = config.hideSeries,
                                onCheckedChange = { onConfigChange(config.copy(hideSeries = it)) },
                                label = stringResource(R.string.panel_fight_hide_series)
                            )
                        }
                        item {
                            // 游戏掉线时自动重连
                            CheckBoxWithExpandableTip(
                                checked = config.autoRestartOnDrop,
                                onCheckedChange = { onConfigChange(config.copy(autoRestartOnDrop = it)) },
                                label = stringResource(R.string.panel_fight_auto_restart_on_drop),
                                tipText = stringResource(R.string.panel_fight_auto_restart_on_drop_tip)
                            )
                        }
                        item {
                            WeeklyScheduleSection(config, onConfigChange)
                        }
                            }
                        }
    }
}

/**
 * 代理倍率选择区域
 * 使用 RadioButton 单选按钮组，FlowRow 自动换行
 */
@Composable
private fun SeriesSection(
    config: FightConfig,
    clientType: String,
    onConfigChange: (FightConfig) -> Unit
) {
    val locked = remember(clientType) { SeriesLock.isLocked(clientType) }
    val displayedSeries = if (locked) -1 else config.series

    SegmentedSettingsGroup {
        item {
            SettingDropdown(
                title = stringResource(R.string.panel_fight_series_title),
                selected = displayedSeries,
                options = UiUsageConstants.seriesOptions.map { it.first },
                optionLabel = { value ->
                    if (value == -1) {
                        stringResource(R.string.panel_fight_series_no_switch)
                    } else {
                        UiUsageConstants.seriesOptions.firstOrNull { it.first == value }?.second
                            ?: value.toString()
                    }
                },
                onSelected = { onConfigChange(config.copy(series = it)) },
                icon = null,
                enabled = !locked,
            )
        }
        if (locked) {
            item {
                Text(
                    text = stringResource(R.string.panel_fight_series_locked_tip),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * 未开放关卡重置策略选择
 * 迁移自 WPF FightStageResetMode 下拉框
 */
@Composable
private fun StageResetModeSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit
) {
    val options = listOf(
        StageResetMode.CURRENT to stringResource(R.string.panel_fight_stage_reset_current),
        StageResetMode.IGNORE to stringResource(R.string.panel_fight_stage_reset_ignore)
    )

    SettingDropdown(
        title = stringResource(R.string.panel_fight_stage_reset_title),
        selected = config.stageResetMode,
        options = options.map { it.first },
        optionLabel = { value -> options.firstOrNull { it.first == value }?.second.orEmpty() },
        onSelected = { onConfigChange(config.copy(stageResetMode = it)) },
        icon = null,
    )
}

/**
 * 分组关卡选择区域（新版）
 * 支持活动关卡和常驻关卡分组显示
 *
 * @param stageGroups 分组后的关卡列表
 */
@Composable
private fun GroupedStageSelectionSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit,
    stageGroups: List<StageGroup>,
    activityManager: ActivityManager
) {
    var tipExpanded by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }

    // 扁平的关卡代码列表（用于输入框模式）
    val stageCodes = remember(stageGroups) {
        stageGroups.flatMap { group -> group.stages.map { it.code } }
    }

    // 首选关卡今日是否开放：经 activityManager.isStageOpen（getStageInfo 兜底）判定，对齐 WPF
    // StageManager.IsStageOpen —— 主线关卡等不在候选列表中的关卡按常规关卡处理，视为开放；空关卡视为开放
    val stage1Open = config.stage1.isBlank() || activityManager.isStageOpen(config.stage1)
    val annihilationOptions = localizedAnnihilationOptions()

    // 当前执行关卡：直接复用 config.getActiveStage(activityManager)，与实际下发 core 的选关完全一致
    // （对齐 WPF：tip 与 SerializeTask 共用 GetFightStage，避免「显示」与「执行」分叉）
    val executingStage = remember(
        config.stage1, config.alternateStages, config.useAlternateStage,
        config.customStageCode, config.stageResetMode, stageGroups, activityManager
    ) {
        config.getActiveStage(activityManager)
    }
    val defaultStageLabel = stringResource(R.string.panel_fight_stage_reset_current)

    // 备选关卡是否会被常驻/当前关卡静默阻断（对齐 WPF PermanentStageBlocksStages）。
    // getActiveStage 选关为「从上往下取第一个今日开放」，常驻关卡每天都开放、stage1 为空（当前/上次）
    // 时更会直接 return ""，两种情况下其后配置的备选关卡都永远不会被执行，需提示用户。
    val alternatesBlocked = remember(
        config.stage1, config.alternateStages,
        config.useAlternateStage, executingStage
    ) {
        if (!config.useAlternateStage) return@remember false
        val alternates = config.alternateStages
        // 当前/上次：getActiveStage 在 stage1 为空时直接返回 ""，备选整体失效
        if (config.stage1.isEmpty()) return@remember alternates.any { it.isNotEmpty() }
        // 执行关卡为常驻关卡时，其后配置的备选关卡永远不会被选中
        if (!activityManager.isPermanentStage(executingStage)) return@remember false
        val candidates = (listOf(config.stage1) + config.alternateStages)
            .filter { it.isNotEmpty() }
        candidates.indexOf(executingStage) in 0 until candidates.lastIndex
    }

    val stagePlanTipText = buildString {
        append(stringResource(R.string.panel_fight_stage_plan_tip))
        if (config.customStageCode) {
            append("\n\n")
            append(stringResource(R.string.panel_fight_stage_selection_tip))
        }
    }

    // 选关告警，互斥优先级：备选被阻断 > 首选不开放(有备选) > 首选不开放。
    // 优先级保证「将使用备选」与「备选不会执行」不会同时出现，消除文案矛盾。
    val stageWarning = when {
        alternatesBlocked ->
            stringResource(R.string.panel_fight_permanent_stage_blocks_alternate)
        !stage1Open && config.stage1.isNotBlank() && config.useAlternateStage ->
            stringResource(R.string.panel_fight_primary_stage_closed_with_alternate, config.stage1)
        !stage1Open && config.stage1.isNotBlank() ->
            stringResource(R.string.panel_fight_primary_stage_closed, config.stage1)
        else -> null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column {
            SettingRow(
                title = stringResource(R.string.panel_fight_stage_selection_title),
                icon = null,
                onClick = { detailsExpanded = !detailsExpanded },
                titleContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.panel_fight_stage_selection_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.panel_fight_current_execution_label),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                StageBadge(text = executingStage.ifEmpty { defaultStageLabel })
                            }
                        }
                        ExpandableTipIcon(
                            expanded = tipExpanded,
                            onExpandedChange = { tipExpanded = it },
                        )
                    }
                },
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = stringResource(
                            if (detailsExpanded) R.string.common_collapse else R.string.common_expand,
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (detailsExpanded) 180f else 0f),
                    )
                },
            )
            ExpandableTipContent(
                visible = tipExpanded,
                tipText = stagePlanTipText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            AnimatedVisibility(
                visible = detailsExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                if (stageWarning != null) {
                    Text(
                        text = "· $stageWarning",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                // 首选关卡：无删除按钮，标题行内用等宽占位与备选关卡对齐左右宽度
                if (config.customStageCode) {
                    // 文本输入模式
                    StageRow(onRemove = null) {
                        StageInputField(
                            value = config.stage1,
                            onValueChange = { onConfigChange(config.copy(stage1 = it)) },
                            label = stringResource(R.string.panel_fight_primary_stage_label),
                            placeholder = stringResource(R.string.panel_fight_primary_stage_placeholder),
                            stageCodes = stageCodes,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    StageOptionGroups(
                        selectedValue = config.stage1,
                        stageGroups = stageGroups,
                        onItemSelected = { onConfigChange(config.copy(stage1 = it)) },
                        annihilationDisplayName = if (config.useCustomAnnihilation) {
                            annihilationOptions
                                .firstOrNull { it.second == config.annihilationStage }
                                ?.first
                        } else null,
                    )
                }

                // 备选关卡（UseAlternateStage 启用时显示，可动态增删）
                if (config.useAlternateStage) {
                    // 更新指定序号的备选关卡
                    fun updateAlternate(index: Int, value: String) {
                        onConfigChange(
                            config.copy(
                                alternateStages = config.alternateStages.toMutableList().also { it[index] = value }
                            )
                        )
                    }
                    // 删除指定序号的备选关卡
                    fun removeAlternate(index: Int) {
                        onConfigChange(
                            config.copy(
                                alternateStages = config.alternateStages.toMutableList().also { it.removeAt(index) }
                            )
                        )
                    }

                    config.alternateStages.forEachIndexed { index, stage ->
                        val alternateLabel = stringResource(R.string.panel_fight_alternate_stage_label, index + 1)
                        if (config.customStageCode) {
                            // 文本输入模式
                            StageRow(onRemove = { removeAlternate(index) }) {
                                StageInputField(
                                    value = stage,
                                    onValueChange = { updateAlternate(index, it) },
                                    label = alternateLabel,
                                    placeholder = stringResource(R.string.panel_fight_alternate_stage_placeholder),
                                    stageCodes = stageCodes,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // 分组按钮选择模式：删除按钮内嵌在折叠标题行内
                            GroupedStageButtonGroup(
                                label = alternateLabel,
                                selectedValue = stage,
                                stageGroups = stageGroups,
                                onItemSelected = { updateAlternate(index, it) },
                                onRemove = { removeAlternate(index) }
                            )
                        }
                    }

                    // 添加备选关卡
                    AddAlternateStageButton(
                        onClick = { onConfigChange(config.copy(alternateStages = config.alternateStages + "")) }
                    )
                }
                }
            }
        }
    }
}

/**
 * 已选关卡徽章：主色底、白字、圆角
 * 与选关状态卡片「当前执行」徽章共用同一样式
 */
@Composable
private fun StageBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * 分组关卡选择按钮组（可折叠）
 * 标题行：左侧区块名，右侧「已选关卡」徽章 + 展开/收起箭头；点击标题行切换折叠
 * 展开后显示分组标题 + 每个分组下的关卡自动换行平铺
 * 默认折叠
 */
@Composable
private fun GroupedStageButtonGroup(
    label: String,
    selectedValue: String,
    stageGroups: List<StageGroup>,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    annihilationDisplayName: String? = null,
    onRemove: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val defaultLabel = stringResource(R.string.panel_fight_stage_reset_current)
    // 已选关卡的展示名：空=当前/上次；自定义剿灭替换名；否则从分组里查显示名，查不到回退关卡代码
    val selectedDisplay = when {
        selectedValue.isEmpty() -> defaultLabel
        selectedValue == "Annihilation" && annihilationDisplayName != null -> annihilationDisplayName
        else -> stageGroups.firstNotNullOfOrNull { group ->
            group.stages.firstOrNull { it.code == selectedValue }?.displayName
        } ?: selectedValue
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        // 折叠标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            StageBadge(text = selectedDisplay)
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            // 删除按钮 / 等宽占位内嵌在标题行内：随标题行 CenterVertically 居中，展开时不会漂移到内容中部
            StageRowTrailing(onRemove)
        }

        // 分组内容（折叠时隐藏）
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            StageOptionGroups(
                selectedValue = selectedValue,
                stageGroups = stageGroups,
                onItemSelected = onItemSelected,
                annihilationDisplayName = annihilationDisplayName,
            )
        }
    }
}

/**
 * 关卡行（自定义关卡代码文本输入模式用）：内容区 + 右侧删除按钮 / 等宽占位
 * onRemove 为空时（首选关卡）渲染等宽占位，保证与备选关卡左右宽度对齐
 * 分组按钮模式的删除按钮内嵌在 GroupedStageButtonGroup 折叠标题行内，不走此容器
 */
@Composable
private fun StageRow(
    onRemove: (() -> Unit)?,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        content()
        StageRowTrailing(onRemove)
    }
}

/**
 * 关卡行尾部控件：删除按钮（onRemove 非空）或等宽占位（首选关卡，保证左右对齐）
 * 对齐 WPF StagePlan 列表项的删除交互（移动端改为常显删除图标，无 hover）
 */
@Composable
private fun StageRowTrailing(
    onRemove: (() -> Unit)?
) {
    if (onRemove != null) {
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.panel_fight_remove_stage),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        // 与删除按钮同宽的占位（IconButton size 36dp），保证首选关卡与备选关卡左右对齐
        Spacer(modifier = Modifier.size(36.dp))
    }
}

/**
 * 添加备选关卡按钮
 * 对齐 WPF AddStageToPlan
 */
@Composable
private fun AddAlternateStageButton(
    onClick: () -> Unit
) {
    SettingActionButton(
        title = stringResource(R.string.panel_fight_add_alternate_stage),
        icon = Icons.Default.Add,
        onClick = onClick,
    )
}

/**
 * 自定义剿灭区域
 * 使用 RadioButton 按钮组替代下拉框
 */
@Composable
private fun CustomAnnihilationSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit
) {

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CheckBoxWithLabel(
            checked = config.useCustomAnnihilation,
            onCheckedChange = { onConfigChange(config.copy(useCustomAnnihilation = it)) },
            label = stringResource(R.string.panel_fight_use_custom_annihilation)
        )

        // 剿灭关卡选择（启用时显示）
        AnimatedVisibility(
            visible = config.useCustomAnnihilation,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            val options = localizedAnnihilationOptions()
            SettingDropdown(
                title = stringResource(R.string.panel_fight_annihilation_title),
                selected = config.annihilationStage,
                options = options.map { it.second },
                optionLabel = { value -> options.firstOrNull { it.second == value }?.first.orEmpty() },
                onSelected = { onConfigChange(config.copy(annihilationStage = it)) },
                icon = null,
            )
        }
    }
}

@Composable
private fun localizedAnnihilationOptions(): List<Pair<String, String>> {
    return listOf(
        stringResource(R.string.panel_fight_annihilation_current) to "Annihilation",
        stringResource(R.string.panel_fight_annihilation_chernobog) to "Chernobog@Annihilation",
        stringResource(R.string.panel_fight_annihilation_outskirts) to "LungmenOutskirts@Annihilation",
        stringResource(R.string.panel_fight_annihilation_downtown) to "LungmenDowntown@Annihilation",
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeeklyScheduleSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit
) {
    val weekDays = listOf(
        "MONDAY" to stringResource(R.string.panel_fight_weekday_monday),
        "TUESDAY" to stringResource(R.string.panel_fight_weekday_tuesday),
        "WEDNESDAY" to stringResource(R.string.panel_fight_weekday_wednesday),
        "THURSDAY" to stringResource(R.string.panel_fight_weekday_thursday),
        "FRIDAY" to stringResource(R.string.panel_fight_weekday_friday),
        "SATURDAY" to stringResource(R.string.panel_fight_weekday_saturday),
        "SUNDAY" to stringResource(R.string.panel_fight_weekday_sunday),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CheckBoxWithExpandableTip(
            checked = config.useWeeklySchedule,
            onCheckedChange = { onConfigChange(config.copy(useWeeklySchedule = it)) },
            label = stringResource(R.string.panel_fight_weekly_schedule),
            tipText = stringResource(R.string.panel_fight_weekly_schedule_tip)
        )
        AnimatedVisibility(
            visible = config.useWeeklySchedule,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                weekDays.forEach { (key, display) ->
                    val selected = config.weeklySchedule[key] != false
                    Surface(
                        onClick = {
                            val updated = config.weeklySchedule.toMutableMap()
                            updated[key] = !selected
                            onConfigChange(config.copy(weeklySchedule = updated))
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = display,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
