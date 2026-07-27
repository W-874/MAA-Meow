package com.aliothmoon.maameow.presentation.view.panel

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.presentation.LocalFloatingWindowContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.utils.Misc
import com.aliothmoon.maameow.constant.MaaApi
import com.aliothmoon.maameow.data.config.MaaPathConfig
import com.aliothmoon.maameow.data.model.CustomInfrastConfig
import com.aliothmoon.maameow.data.model.InfrastConfig
import com.aliothmoon.maameow.domain.enums.InfrastMode
import com.aliothmoon.maameow.domain.enums.InfrastRoomType
import com.aliothmoon.maameow.domain.enums.UiUsageConstants
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipContent
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipIcon
import com.aliothmoon.maameow.presentation.components.CheckBoxWithExpandableTip
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingDropdown
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.components.ReorderableFlowRow
import com.aliothmoon.maameow.utils.JsonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 基建换班配置面板
 */
@Composable
fun InfrastConfigPanel(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit, modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_general)) }
                        item { SegmentedSettingsGroup {
                            item {
                                InfrastModeSection(config, onConfigChange)
                            }
                            item {
                                AnimatedVisibility(
                                    visible = config.mode == InfrastMode.Custom,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    CustomInfrastSection(config, onConfigChange)
                                }
                            }
                            item {
                                AnimatedVisibility(
                                    visible = config.mode != InfrastMode.Custom,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    UsesOfDronesSection(config, onConfigChange)
                                }
                            }
                            item {
                                AnimatedVisibility(
                                    visible = config.mode != InfrastMode.Rotation,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    DormThresholdSection(config, onConfigChange)
                                }
                            }
                            item {
                                FacilitiesSection(config, onConfigChange)
                            }
                        } }
        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_advanced)) }
                        item { SegmentedSettingsGroup {
                            item {
                                    AnimatedVisibility(
                                        visible = config.mode != InfrastMode.Rotation,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                    ) {
                                        DormTrustEnabledSection(config, onConfigChange)
                                    }
                            }
                            item {
                                    AnimatedVisibility(
                                        visible = config.mode != InfrastMode.Rotation,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                    ) {
                                        DormFilterNotStationedSection(config, onConfigChange)
                                    }
                            }
                            item { OriginiumShardAutoReplenishmentSection(config, onConfigChange) }
                            item { ReceptionMessageBoardReceiveSection(config, onConfigChange) }
                            item { ReceptionClueExchangeSection(config, onConfigChange) }
                            item { ReceptionSendClueSection(config, onConfigChange) }
                            item { ContinueTrainingSection(config, onConfigChange) }
                        } }
    }
}

/**
 * 基建模式选择区域
 */
@Composable
private fun InfrastModeSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingDropdown(
            title = stringResource(R.string.panel_infrast_mode_title),
            selected = config.mode,
            options = InfrastMode.values.toList(),
            optionLabel = { infrastModeLabel(it) },
            onSelected = { onConfigChange(config.copy(mode = it)) },
            icon = null,
        )

        // Rotation 模式提示文字
        AnimatedVisibility(
            visible = config.mode == InfrastMode.Rotation,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.panel_infrast_mode_rotation_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

/**
 * 自定义基建配置区域（仅 Custom 模式显示）
 */
@Composable
private fun CustomInfrastSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    val pathConfig: MaaPathConfig = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isInFloatingWindow = LocalFloatingWindowContext.current

    // SAF 文件选择器（悬浮窗环境下不可用）
    val filePicker = if (!isInFloatingWindow) {
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch {
                withContext(Dispatchers.IO) {
                    val destDir = File(pathConfig.rootDir, "custom_infrast").apply { mkdirs() }
                    val rawName = queryFileName(context, uri) ?: "user_infrast.json"
                    val nameWithoutExt = rawName.substringBeforeLast(".")
                    val ext = rawName.substringAfterLast(".", "json")
                    val hash = Integer.toHexString(nameWithoutExt.hashCode()).takeLast(6)
                    val safeName = "${nameWithoutExt}_${hash}.${ext}"
                        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val destFile = File(destDir, safeName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    onConfigChange(
                        config.copy(
                            defaultInfrast = UiUsageConstants.USER_DEFINED_INFRAST,
                            customInfrastFile = destFile.absolutePath,
                            customInfrastPlanSelect = -1
                        )
                    )
                }
            }
        }
    } else null

    // 解析后的配置（用于计划下拉框）
    val (custom, setCustom) = remember { mutableStateOf<CustomInfrastConfig?>(null) }
    val (error, setError) = remember { mutableStateOf<String?>(null) }
    val fileNotFoundMsg = stringResource(R.string.panel_infrast_file_not_found)
    val parseFailedFmt = stringResource(R.string.panel_infrast_parse_failed, "%s")

    // 当文件路径变化时解析配置
    LaunchedEffect(config.customInfrastFile) {
        if (config.customInfrastFile.isBlank()) {
            setCustom(null)
            setError(null)
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val file = File(config.customInfrastFile)
                if (file.exists()) {
                    val content = file.readText()
                    val parsed = JsonUtils.common.decodeFromString<CustomInfrastConfig>(content)
                    setCustom(parsed)
                    setError(null)
                    // 同步时间段数据 + 计划名列表 + 排班计划存在时自动选中第一个
                    val periods = parsed.plans.map { it.period }
                    val names = parsed.plans.mapIndexed { index, plan ->
                        plan.name ?: "Plan ${('A' + index)}"
                    }
                    val hasPeriodicPlan = parsed.plans.any { it.period.isNotEmpty() }
                    val autoSelect = !hasPeriodicPlan
                            && config.customInfrastPlanSelect == -1
                            && parsed.plans.isNotEmpty()
                    if (autoSelect
                        || periods != config.customPlanPeriods
                        || names != config.customPlanNames
                    ) {
                        val planIndex = if (autoSelect) 0 else config.customInfrastPlanSelect
                        onConfigChange(
                            config.copy(
                                customPlanPeriods = periods,
                                customPlanNames = names,
                                customInfrastPlanSelect = planIndex
                            )
                        )
                    }
                } else {
                    setCustom(null)
                    setError(fileNotFoundMsg)
                }
            } catch (e: Exception) {
                setCustom(null)
                setError(parseFailedFmt.format(e.message.orEmpty()))
            }
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 配置信息卡片（仅当 custom 有 title/description 时显示）
        if (custom != null && custom.plans.isNotEmpty()) {
            if (!custom.title.isNullOrBlank() || !custom.description.isNullOrBlank()) {
                var descExpanded by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ), modifier = Modifier.clickable(
                        enabled = !custom.description.isNullOrBlank()
                    ) { descExpanded = !descExpanded }) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!custom.title.isNullOrBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = custom.title.replace("\\n", "\n"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!custom.description.isNullOrBlank()) {
                                    ExpandableTipIcon(
                                        expanded = descExpanded,
                                        onExpandedChange = { descExpanded = it })
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = descExpanded && !custom.description.isNullOrBlank(),
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Text(
                                text = custom.description?.replace("\\n", "\n") ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 在线生成器链接
        val context = LocalContext.current
        Text(
            text = stringResource(R.string.panel_infrast_scheduler_builder),
            style = MaterialTheme.typography.bodySmall.copy(
                textDecoration = TextDecoration.Underline
            ), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable {
                Misc.openUriSafely(context, MaaApi.BASE_SCHEDULING_SCHEMA)
            })

        val importBackgroundOnlyMessage =
            stringResource(R.string.panel_infrast_import_background_only)

        // 内置配置选择
        PresetButtonGroup(
            selectedPreset = config.defaultInfrast, onPresetSelected = { preset ->
                if (preset == UiUsageConstants.USER_DEFINED_INFRAST) {
                    if (filePicker != null) {
                        filePicker.launch(arrayOf("application/json"))
                    } else {
                        Toast.makeText(
                            context,
                            importBackgroundOnlyMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val filePath = File(
                        pathConfig.resourceDir, "custom_infrast/$preset"
                    ).absolutePath
                    onConfigChange(
                        config.copy(
                            defaultInfrast = preset,
                            customInfrastFile = filePath,
                            customInfrastPlanSelect = -1
                        )
                    )
                }
            })

        // 排班计划选择
        if (custom != null && custom.plans.isNotEmpty()) {
            PlanSelectButtonGroup(
                plans = custom.plans,
                selectedPlanIndex = config.customInfrastPlanSelect,
                onPlanSelected = {
                    onConfigChange(config.copy(customInfrastPlanSelect = it))
                })
        }

        //  解析错误提示
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * 内置预设按钮组
 */
@Composable
private fun PresetButtonGroup(
    selectedPreset: String, onPresetSelected: (String) -> Unit
) {
    val options = UiUsageConstants.defaultInfrastPresets.map { it.first }
    SettingDropdown(
        title = stringResource(R.string.panel_infrast_presets_title),
        selected = selectedPreset,
        options = options,
        optionLabel = { infrastPresetLabel(it) },
        onSelected = onPresetSelected,
        icon = null,
    )
}

/**
 * 计划选择按钮组
 */
@Composable
private fun PlanSelectButtonGroup(
    plans: List<CustomInfrastConfig.Plan>, selectedPlanIndex: Int, onPlanSelected: (Int) -> Unit
) {
    val hasPeriodicPlan = plans.any { it.period.isNotEmpty() }
    val hasNonPeriodicPlan = plans.any { it.period.isEmpty() }

    // 计算当前时间匹配的计划名（用于时间轮换显示）
    // TODO: 定时刷新时间轮换显示（WPF 每分钟调用 RefreshInfrastTimeRotationDisplay 更新）
    val currentPlanName = if (hasPeriodicPlan) {
        val now = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("H:mm")
        val matched = plans.firstOrNull { plan ->
            plan.period.any { range ->
                if (range.size < 2) return@any false
                val start = runCatching { LocalTime.parse(range[0], formatter) }.getOrNull()
                    ?: return@any false
                val end = runCatching { LocalTime.parse(range[1], formatter) }.getOrNull()
                    ?: return@any false
                if (start <= end) now in start..end
                else now >= start || now <= end
            }
        }
        matched?.name ?: plans.firstOrNull()?.name ?: "???"
    } else null

    val currentPlanDisplayName = currentPlanName ?: "???"
    val autoSwitchLabel = stringResource(
        R.string.panel_infrast_plan_auto_switch,
        currentPlanDisplayName,
    )
    val optionIndexes = buildList {
        if (hasPeriodicPlan) add(-1)
        addAll(plans.indices)
    }
    val optionLabels = plans.mapIndexed { index, plan ->
            val periodText = if (plan.period.isNotEmpty()) {
                plan.period.joinToString(", ") { range ->
                    if (range.size >= 2) "${range[0]}-${range[1]}" else ""
                }
            } else ""
            index to buildString {
                append(plan.name ?: "Plan ${'A' + index}")
                if (periodText.isNotBlank()) append(" ($periodText)")
            }
    }.toMap()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingDropdown(
            title = stringResource(R.string.panel_infrast_plan_title),
            selected = selectedPlanIndex.takeIf { it in optionIndexes } ?: optionIndexes.first(),
            options = optionIndexes,
            optionLabel = { index ->
                if (index == -1) autoSwitchLabel else optionLabels[index].orEmpty()
            },
            onSelected = onPlanSelected,
            icon = null,
        )

        // 当前选中计划的描述
        if (selectedPlanIndex >= 0 && selectedPlanIndex < plans.size) {
            val desc = plans[selectedPlanIndex].description
            if (!desc.isNullOrBlank()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // 部分计划无时间段警告
        if (hasPeriodicPlan && hasNonPeriodicPlan) {
            Text(
                text = stringResource(R.string.panel_infrast_plan_missing_period_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun infrastModeLabel(mode: InfrastMode): String {
    return when (mode) {
        InfrastMode.Normal -> stringResource(R.string.panel_infrast_mode_normal)
        InfrastMode.Custom -> stringResource(R.string.panel_infrast_mode_custom)
        InfrastMode.Rotation -> stringResource(R.string.panel_infrast_mode_rotation)
    }
}

@Composable
private fun infrastPresetLabel(key: String): String {
    return when (key) {
        UiUsageConstants.USER_DEFINED_INFRAST -> stringResource(R.string.panel_infrast_preset_user_defined)
        "153_layout_3_times_a_day.json" -> stringResource(R.string.panel_infrast_preset_153_3x)
        "153_layout_4_times_a_day.json" -> stringResource(R.string.panel_infrast_preset_153_4x)
        "243_layout_3_times_a_day.json" -> stringResource(R.string.panel_infrast_preset_243_3x)
        "243_layout_4_times_a_day.json" -> stringResource(R.string.panel_infrast_preset_243_4x)
        "333_layout_for_Orundum_3_times_a_day.json" -> stringResource(R.string.panel_infrast_preset_333_3x)
        else -> key
    }
}

@Composable
private fun infrastRoomTypeLabel(roomType: InfrastRoomType): String {
    return when (roomType) {
        InfrastRoomType.Mfg -> stringResource(R.string.panel_infrast_room_mfg)
        InfrastRoomType.Trade -> stringResource(R.string.panel_infrast_room_trade)
        InfrastRoomType.Control -> stringResource(R.string.panel_infrast_room_control)
        InfrastRoomType.Power -> stringResource(R.string.panel_infrast_room_power)
        InfrastRoomType.Reception -> stringResource(R.string.panel_infrast_room_reception)
        InfrastRoomType.Office -> stringResource(R.string.panel_infrast_room_office)
        InfrastRoomType.Dorm -> stringResource(R.string.panel_infrast_room_dorm)
        InfrastRoomType.Processing -> stringResource(R.string.panel_infrast_room_processing)
        InfrastRoomType.Training -> stringResource(R.string.panel_infrast_room_training)
    }
}

@Composable
private fun localizedDroneUsageOptions(): List<Pair<String, String>> {
    return UiUsageConstants.droneUsageValues.map { usage ->
        usage to when (usage) {
            "_NotUse" -> stringResource(R.string.panel_infrast_drones_not_use)
            "Money" -> stringResource(R.string.panel_infrast_drones_money)
            "SyntheticJade" -> stringResource(R.string.panel_infrast_drones_synthetic_jade)
            "CombatRecord" -> stringResource(R.string.panel_infrast_drones_combat_record)
            "PureGold" -> stringResource(R.string.panel_infrast_drones_pure_gold)
            "OriginStone" -> stringResource(R.string.panel_infrast_drones_origin_stone)
            "Chip" -> stringResource(R.string.panel_infrast_drones_chip)
            else -> usage
        }
    }
}

/**
 * 无人机用途选择区域
 * 使用 RadioButton 单选按钮组，FlowRow 自动换行
 */
@Composable
private fun UsesOfDronesSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    val options = localizedDroneUsageOptions()
    SettingDropdown(
        title = stringResource(R.string.panel_infrast_drones_title),
        selected = config.usesOfDrones,
        options = options.map { it.first },
        optionLabel = { value -> options.firstOrNull { it.first == value }?.second ?: value },
        onSelected = { onConfigChange(config.copy(usesOfDrones = it)) },
        icon = null,
    )
}

/**
 * 心情阈值设置区域
 */
@Composable
private fun DormThresholdSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    var tipExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.panel_infrast_dorm_threshold_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                ExpandableTipIcon(
                    expanded = tipExpanded, onExpandedChange = { tipExpanded = it })
            }
            Text(
                text = "${config.dormThreshold}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        ExpandableTipContent(
            visible = tipExpanded,
            tipText = stringResource(R.string.panel_infrast_dorm_threshold_tip)
        )

        Slider(
            value = config.dormThreshold.toFloat(),
            onValueChange = { onConfigChange(config.copy(dormThreshold = it.toInt())) },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 设施列表区域
 */
@Composable
private fun FacilitiesSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.panel_infrast_facilities_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.panel_infrast_facilities_tip_fixed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReorderableFlowRow(
            items = config.facilities,
            itemKey = { it.first },
            onOrderChanged = { onConfigChange(config.copy(facilities = it)) },
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { (facility, selected), _, dragModifier ->
            FilterChip(
                selected = selected,
                onClick = {
                    onConfigChange(
                        config.copy(
                            facilities = config.facilities.map {
                                if (it.first == facility) it.first to !selected else it
                            },
                        ),
                    )
                },
                label = {
                    Text(
                        text = infrastRoomTypeLabel(facility),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = null,
                modifier = dragModifier.height(30.dp),
            )
        }
    }
}

/**
 * 宿舍信赖模式（仅Normal模式显示）
 */
@Composable
private fun DormTrustEnabledSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    CheckBoxWithLabel(
        checked = config.dormTrustEnabled,
        onCheckedChange = { onConfigChange(config.copy(dormTrustEnabled = it)) },
        label = stringResource(R.string.panel_infrast_dorm_trust),
    )
}

/**
 * 不将已进驻干员放入宿舍（仅Normal模式显示）
 */
@Composable
private fun DormFilterNotStationedSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    CheckBoxWithExpandableTip(
        checked = config.dormFilterNotStationedEnabled,
        onCheckedChange = { onConfigChange(config.copy(dormFilterNotStationedEnabled = it)) },
        label = stringResource(R.string.panel_infrast_dorm_filter_not_stationed),
        tipText = stringResource(R.string.panel_infrast_dorm_filter_not_stationed_tip),
    )
}

/**
 * 制造站搓玉自动补货
 */
@Composable
private fun OriginiumShardAutoReplenishmentSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    CheckBoxWithLabel(config.originiumShardAutoReplenishment, { onConfigChange(config.copy(originiumShardAutoReplenishment = it)) }, stringResource(R.string.panel_infrast_originium_shard_auto_replenishment))
}

/**
 * 会客室留言板领取信用
 */
@Composable
private fun ReceptionMessageBoardReceiveSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    CheckBoxWithLabel(config.receptionMessageBoard, { onConfigChange(config.copy(receptionMessageBoard = it)) }, stringResource(R.string.panel_infrast_reception_message_board))
}

/**
 * 会客室线索交流
 */
@Composable
private fun ReceptionClueExchangeSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    CheckBoxWithLabel(config.receptionClueExchange, { onConfigChange(config.copy(receptionClueExchange = it)) }, stringResource(R.string.panel_infrast_reception_clue_exchange))
}

/**
 * 会客室赠送线索
 */
@Composable
private fun ReceptionSendClueSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    CheckBoxWithLabel(config.receptionSendClue, { onConfigChange(config.copy(receptionSendClue = it)) }, stringResource(R.string.panel_infrast_reception_send_clue))
}

/**
 * 继续专精
 */
@Composable
private fun ContinueTrainingSection(
    config: InfrastConfig, onConfigChange: (InfrastConfig) -> Unit
) {
    CheckBoxWithLabel(config.continueTraining, { onConfigChange(config.copy(continueTraining = it)) }, stringResource(R.string.panel_infrast_continue_training))
}

private fun queryFileName(context: Context, uri: Uri): String? = Misc.queryFileName(context, uri)
