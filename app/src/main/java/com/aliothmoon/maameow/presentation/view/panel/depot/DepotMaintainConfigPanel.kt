package com.aliothmoon.maameow.presentation.view.panel.depot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.presentation.view.panel.LocalTaskPanelBottomPadding
import com.aliothmoon.maameow.data.model.DepotMaintainConfig
import com.aliothmoon.maameow.data.model.DepotMaintainPlan
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.resource.ActivityManager
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.StageGroup
import com.aliothmoon.maameow.domain.enums.UiUsageConstants
import com.aliothmoon.maameow.presentation.components.CheckBoxWithExpandableTip
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.INumericField
import com.aliothmoon.maameow.presentation.components.NumberStepperSettingRow
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.components.SettingActionButton
import com.aliothmoon.maameow.presentation.components.animatedSegmentedItemShape
import com.aliothmoon.maameow.presentation.view.panel.TaskSettingsSectionTitle
import com.aliothmoon.maameow.presentation.view.panel.common.StageInputField
import com.aliothmoon.maameow.presentation.view.panel.common.StageOptionGroups
import org.koin.compose.koinInject

/** 目标库存上限，对齐 WPF NumericUpDown 的 Maximum */
private const val MAX_TARGET_INVENTORY = 1145141919

@Composable
fun DepotMaintainConfigPanel(
    config: DepotMaintainConfig,
    onConfigChange: (DepotMaintainConfig) -> Unit,
    modifier: Modifier = Modifier,
    depotRepository: DepotRepository = koinInject(),
    itemHelper: ItemHelper = koinInject(),
    activityManager: ActivityManager = koinInject(),
) {
    val snapshot by depotRepository.snapshot.collectAsStateWithLifecycle()
    val dropItems by itemHelper.dropItems.collectAsStateWithLifecycle()
    val activityStages by activityManager.activityStages.collectAsStateWithLifecycle()

    // 排除「当期剿灭」：库存保持按材料刷取，剿灭无指定掉落。对齐上游 RefreshStageList
    val stageGroups = remember(activityStages) {
        activityManager.getMergedStageGroups()
            .map { group -> group.copy(stages = group.stages.filterNot { it.code == "Annihilation" }) }
            .filter { it.stages.isNotEmpty() }
    }
    val stageCodes = remember(stageGroups) {
        stageGroups.flatMap { group -> group.stages.map { it.code } }
    }
    val itemNameMap = remember(dropItems) { dropItems.associate { it.id to it.name } }
    val itemIds = if (dropItems.isNotEmpty()) dropItems.map { it.id } else UiUsageConstants.dropItems

    // 展开态是纯 UI 局部状态，不持久化；删除时重映射下标，避免落到相邻计划。
    val expandedIndices = remember { mutableStateListOf<Int>() }
    val expandedStageIndices = remember { mutableStateListOf<Int>() }
    val expandedMaterialIndices = remember { mutableStateListOf<Int>() }

    val notSelectedLabel = stringResource(R.string.panel_depot_not_selected)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = 4.dp,
                bottom = LocalTaskPanelBottomPadding.current,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TaskSettingsSectionTitle(stringResource(R.string.common_tab_general))
        SegmentedSettingsGroup {
            item {
                CheckBoxWithExpandableTip(
                    checked = config.updateDepot,
                    onCheckedChange = { onConfigChange(config.copy(updateDepot = it)) },
                    label = stringResource(R.string.panel_depot_update_before_start),
                    tipText = stringResource(R.string.panel_depot_update_before_start_tip),
                )
            }
            item {
                CheckBoxWithExpandableTip(
                    checked = config.useAutoSeries,
                    onCheckedChange = { onConfigChange(config.copy(useAutoSeries = it)) },
                    label = stringResource(R.string.panel_depot_use_auto_series),
                    tipText = stringResource(R.string.panel_depot_use_auto_series_tip),
                )
            }
            item {
                CheckBoxWithLabel(
                    checked = config.skipDuringActivity,
                    onCheckedChange = { onConfigChange(config.copy(skipDuringActivity = it)) },
                    label = stringResource(R.string.panel_depot_skip_during_activity),
                )
            }
            item {
                CheckBoxWithLabel(
                    checked = config.skipDuringResourceCollection,
                    onCheckedChange = {
                        onConfigChange(config.copy(skipDuringResourceCollection = it))
                    },
                    label = stringResource(R.string.panel_depot_skip_during_resource),
                )
            }
            item {
                CheckBoxWithExpandableTip(
                    checked = config.customStageCode,
                    onCheckedChange = { onConfigChange(config.copy(customStageCode = it)) },
                    label = stringResource(R.string.panel_fight_custom_stage_code),
                    tipText = stringResource(R.string.panel_fight_custom_stage_code_tip),
                )
            }
        }

        TaskSettingsSectionTitle(stringResource(R.string.panel_depot_plans))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val itemCount = config.plans.size + 1
            config.plans.forEachIndexed { index, plan ->
                val expanded = index in expandedIndices
                val stageExpanded = index in expandedStageIndices
                val materialExpanded = index in expandedMaterialIndices
                val planShape = animatedSegmentedItemShape(index, itemCount, expanded)
                val stageShape = animatedSegmentedItemShape(0, 2, stageExpanded)
                val materialShape = animatedSegmentedItemShape(1, 2, materialExpanded)
                val current = if (snapshot.syncTimeMillis == 0L) {
                    "--"
                } else {
                    (snapshot.items[plan.dropId] ?: 0).toString()
                }
                val updatePlan: (DepotMaintainPlan) -> Unit = { updated ->
                    onConfigChange(config.copy(
                        plans = config.plans.toMutableList().also { it[index] = updated },
                    ))
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        onClick = {
                            if (expanded) {
                                expandedIndices.remove(index)
                                expandedStageIndices.remove(index)
                                expandedMaterialIndices.remove(index)
                            } else {
                                expandedIndices.add(index)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = planShape,
                        color = if (expanded) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceBright
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (expanded) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (index + 1).toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (expanded) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemNameMap[plan.dropId] ?: notSelectedLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (expanded) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Medium
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.panel_depot_plan_progress,
                                        stageDisplayOf(stageGroups, plan.stage),
                                        current,
                                        plan.dropCount,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (expanded) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.76f,
                                        )
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = if (expanded) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp),
                        ) {
                            SegmentedSettingsGroup {
                                item(
                                    shape = stageShape,
                                ) {
                                    Column {
                                        SettingRow(
                                            title = stringResource(
                                                R.string.panel_fight_primary_stage_label,
                                            ),
                                            description = stageDisplayOf(stageGroups, plan.stage),
                                            icon = null,
                                            onClick = {
                                                if (stageExpanded) {
                                                    expandedStageIndices.remove(index)
                                                } else {
                                                    expandedStageIndices.add(index)
                                                }
                                            },
                                            trailing = {
                                                Icon(
                                                    imageVector = if (stageExpanded) {
                                                        Icons.Default.ExpandLess
                                                    } else {
                                                        Icons.Default.ExpandMore
                                                    },
                                                    contentDescription = stringResource(
                                                        if (stageExpanded) {
                                                            R.string.common_collapse
                                                        } else {
                                                            R.string.common_expand
                                                        },
                                                    ),
                                                )
                                            },
                                        )
                                        AnimatedVisibility(
                                            visible = stageExpanded,
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically(),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(
                                                    start = 16.dp,
                                                    end = 16.dp,
                                                    bottom = 16.dp,
                                                ),
                                            ) {
                                                if (config.customStageCode) {
                                                    StageInputField(
                                                        value = plan.stage,
                                                        onValueChange = {
                                                            updatePlan(plan.copy(stage = it))
                                                        },
                                                        label = stringResource(
                                                            R.string.panel_fight_primary_stage_label,
                                                        ),
                                                        placeholder = stringResource(
                                                            R.string.panel_fight_primary_stage_placeholder,
                                                        ),
                                                        stageCodes = stageCodes,
                                                        modifier = Modifier.fillMaxWidth(),
                                                    )
                                                } else {
                                                    StageOptionGroups(
                                                        selectedValue = plan.stage,
                                                        stageGroups = stageGroups,
                                                        onItemSelected = {
                                                            updatePlan(plan.copy(stage = it))
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                item(
                                    shape = materialShape,
                                ) {
                                    Column {
                                        SettingRow(
                                            title = stringResource(R.string.panel_fight_material),
                                            description = itemNameMap[plan.dropId]
                                                ?: notSelectedLabel,
                                            icon = null,
                                            onClick = {
                                                if (materialExpanded) {
                                                    expandedMaterialIndices.remove(index)
                                                } else {
                                                    expandedMaterialIndices.add(index)
                                                }
                                            },
                                            trailing = {
                                                Icon(
                                                    imageVector = if (materialExpanded) {
                                                        Icons.Default.ExpandLess
                                                    } else {
                                                        Icons.Default.ExpandMore
                                                    },
                                                    contentDescription = stringResource(
                                                        if (materialExpanded) {
                                                            R.string.common_collapse
                                                        } else {
                                                            R.string.common_expand
                                                        },
                                                    ),
                                                )
                                            },
                                        )
                                        AnimatedVisibility(
                                            visible = materialExpanded,
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically(),
                                        ) {
                                            SingleSelectItemTags(
                                                selectedValue = plan.dropId,
                                                itemIds = listOf("") + itemIds,
                                                itemNameMap = itemNameMap,
                                                notSelectedLabel = notSelectedLabel,
                                                onItemSelected = {
                                                    updatePlan(plan.copy(dropId = it))
                                                },
                                                modifier = Modifier.padding(
                                                    start = 16.dp,
                                                    end = 16.dp,
                                                    bottom = 16.dp,
                                                ),
                                            )
                                        }
                                    }
                                }
                                item {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        INumericField(
                                            value = plan.dropCount,
                                            onValueChange = {
                                                updatePlan(plan.copy(dropCount = it))
                                            },
                                            label = stringResource(
                                                R.string.panel_depot_target_inventory,
                                            ),
                                            minimum = 1,
                                            maximum = MAX_TARGET_INVENTORY,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                item {
                                    CheckBoxWithLabel(
                                        checked = plan.useMedicine,
                                        onCheckedChange = {
                                            updatePlan(plan.copy(useMedicine = it))
                                        },
                                        label = stringResource(
                                            R.string.panel_fight_use_medicine,
                                        ),
                                    )
                                }
                                if (plan.useMedicine) {
                                    item {
                                        NumberStepperSettingRow(
                                            title = stringResource(
                                                R.string.panel_fight_use_medicine_count,
                                            ),
                                            value = plan.medicineCount,
                                            onValueChange = {
                                                updatePlan(plan.copy(medicineCount = it))
                                            },
                                            range = 0..999,
                                            icon = null,
                                        )
                                    }
                                }
                                item {
                                    CheckBoxWithLabel(
                                        checked = plan.useStone,
                                        onCheckedChange = {
                                            updatePlan(plan.copy(useStone = it))
                                        },
                                        label = stringResource(R.string.panel_stone_use),
                                    )
                                }
                                if (plan.useStone) {
                                    item {
                                        NumberStepperSettingRow(
                                            title = stringResource(
                                                R.string.panel_depot_stone_count,
                                            ),
                                            value = plan.stoneCount,
                                            onValueChange = {
                                                updatePlan(plan.copy(stoneCount = it))
                                            },
                                            range = 0..999,
                                            icon = null,
                                        )
                                    }
                                }
                                item {
                                    SettingRow(
                                        title = stringResource(
                                            R.string.panel_depot_remove_plan,
                                        ),
                                        titleColor = MaterialTheme.colorScheme.error,
                                        leadingColor = MaterialTheme.colorScheme.error,
                                        icon = Icons.Default.Delete,
                                        onClick = {
                                            fun remap(indices: List<Int>): List<Int> =
                                                indices.mapNotNull { expandedIndex ->
                                                    when {
                                                        expandedIndex < index -> expandedIndex
                                                        expandedIndex == index -> null
                                                        else -> expandedIndex - 1
                                                    }
                                                }.distinct()

                                            val remappedPlans = remap(expandedIndices)
                                            val remappedStages = remap(expandedStageIndices)
                                            val remappedMaterials = remap(expandedMaterialIndices)
                                            expandedIndices.clear()
                                            expandedIndices.addAll(remappedPlans)
                                            expandedStageIndices.clear()
                                            expandedStageIndices.addAll(remappedStages)
                                            expandedMaterialIndices.clear()
                                            expandedMaterialIndices.addAll(remappedMaterials)
                                            onConfigChange(config.copy(
                                                plans = config.plans.toMutableList().also {
                                                    it.removeAt(index)
                                                },
                                            ))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SettingActionButton(
                title = stringResource(R.string.panel_depot_add_plan),
                icon = Icons.Default.Add,
                onClick = {
                    val newIndex = config.plans.size
                    onConfigChange(config.copy(plans = config.plans + DepotMaintainPlan()))
                    expandedIndices.add(newIndex)
                },
                shape = animatedSegmentedItemShape(
                    index = config.plans.size,
                    itemCount = itemCount,
                    expanded = false,
                ),
            )
        }
    }
}

@Composable
private fun SingleSelectItemTags(
    selectedValue: String,
    itemIds: List<String>,
    itemNameMap: Map<String, String>,
    notSelectedLabel: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemIds.distinct().forEach { itemId ->
            val selected = itemId == selectedValue
            FilterChip(
                selected = selected,
                onClick = { onItemSelected(itemId) },
                label = {
                    Text(
                        text = itemNameMap[itemId] ?: notSelectedLabel,
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
                modifier = Modifier.height(30.dp),
            )
        }
    }
}

/** 关卡代码 → 显示名；查不到回退代码本身，空串回退「未选择」 */
@Composable
private fun stageDisplayOf(stageGroups: List<StageGroup>, code: String): String {
    if (code.isEmpty()) return stringResource(R.string.panel_depot_not_selected)
    return stageGroups.firstNotNullOfOrNull { group ->
        group.stages.firstOrNull { it.code == code }?.displayName
    } ?: code
}
