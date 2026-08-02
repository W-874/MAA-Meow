package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.toolbox.RecruitCalcResult
import com.aliothmoon.maameow.presentation.components.ExpressiveSwitch
import com.aliothmoon.maameow.presentation.components.InfoCard
import com.aliothmoon.maameow.presentation.components.INumericField
import com.aliothmoon.maameow.presentation.components.LocalSettingItemShape
import com.aliothmoon.maameow.presentation.components.animatedSegmentedItemShape
import com.aliothmoon.maameow.presentation.components.NumberStepperSettingRow
import com.aliothmoon.maameow.presentation.components.RecruitConfirmationSettingRow
import com.aliothmoon.maameow.presentation.components.RecruitTimeBadge
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.navigation.LocalMainBottomBarPadding
import com.aliothmoon.maameow.presentation.viewmodel.ToolboxViewModel
import com.aliothmoon.maameow.presentation.viewmodel.RecruitCalcConfig
import com.aliothmoon.maameow.theme.MaaDesignTokens
import com.aliothmoon.maameow.utils.i18n.asString
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecruitCalcPanel(
    modifier: Modifier = Modifier,
    viewModel: ToolboxViewModel = koinInject()
) {
    val tags by viewModel.collector.recruitTags.collectAsStateWithLifecycle()
    val results by viewModel.collector.recruitResults.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val resolvedStatusMessage = statusMessage.asString()
    val config by viewModel.recruitConfig.collectAsStateWithLifecycle()
    val mainBottomBarPadding = LocalMainBottomBarPadding.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaaDesignTokens.Spacing.listHorizontal),
        contentPadding = PaddingValues(
            top = MaaDesignTokens.Spacing.xs,
            bottom = mainBottomBarPadding + MaaDesignTokens.Spacing.xs,
        ),
        verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.sm)
    ) {
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val useTwoColumns = maxWidth >= 600.dp
                if (useTwoColumns) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        maxItemsInEachRow = 2,
                    ) {
                        repeat(5) { index ->
                            RecruitCalcSettingContent(
                                index = index,
                                config = config,
                                onConfigChange = viewModel::onRecruitConfigChange,
                                modifier = Modifier.weight(1f),
                                compact = true,
                            )
                        }
                    }
                } else {
                    SegmentedSettingsGroup {
                        repeat(5) { index ->
                            item {
                                RecruitCalcSettingContent(
                                    index = index,
                                    config = config,
                                    onConfigChange = viewModel::onRecruitConfigChange,
                                    withinSegmentedGroup = true,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 检测到的标签
        if (tags.isNotEmpty()) {
            item {
                InfoCard(title = stringResource(R.string.panel_recruit_calc_detected_tags)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 计算结果
        if (results.isNotEmpty()) {
            val sorted = results.sortedByDescending { it.level }
            items(sorted, key = { it.tags.joinToString() }) { result ->
                RecruitResultItem(result)
            }
        }

        // 空提示
        if (tags.isEmpty() && results.isEmpty()) {
            item {
                InfoCard {
                    Text(
                        text = resolvedStatusMessage.ifBlank {
                            stringResource(R.string.panel_recruit_calc_empty_hint)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecruitCalcSettingContent(
    index: Int,
    config: RecruitCalcConfig,
    onConfigChange: (RecruitCalcConfig) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    withinSegmentedGroup: Boolean = false,
) {
    if (index == 0) {
        RecruitCalcSwitchSetting(
            index = index,
            itemCount = 5,
            title = stringResource(R.string.panel_recruit_calc_auto_time),
            checked = config.autoSetTime,
            onCheckedChange = { onConfigChange(config.copy(autoSetTime = it)) },
            modifier = modifier,
            compact = compact,
            withinSegmentedGroup = withinSegmentedGroup,
        )
        return
    }

    val level = index + 2
    val checked = when (level) {
        3 -> config.chooseLevel3
        4 -> config.chooseLevel4
        5 -> config.chooseLevel5
        else -> config.chooseLevel6
    }
    val time = when (level) {
        3 -> config.level3Time
        4 -> config.level4Time
        else -> 540
    }
    RecruitCalcSwitchSetting(
        index = index,
        itemCount = 5,
        title = stringResource(R.string.panel_recruit_calc_auto_select_tags, level),
        checked = checked,
        onCheckedChange = { nextChecked ->
            val nextConfig = when (level) {
                3 -> config.copy(chooseLevel3 = nextChecked)
                4 -> config.copy(chooseLevel4 = nextChecked)
                5 -> config.copy(chooseLevel5 = nextChecked)
                else -> config.copy(chooseLevel6 = nextChecked)
            }
            onConfigChange(nextConfig)
        },
        showTime = config.autoSetTime,
        timeEditable = level < 5,
        totalMinutes = time,
        onTimeChange = { totalMinutes ->
            val nextConfig = when (level) {
                3 -> config.copy(level3Time = totalMinutes)
                4 -> config.copy(level4Time = totalMinutes)
                else -> config
            }
            onConfigChange(nextConfig)
        },
        modifier = modifier,
        compact = compact,
        withinSegmentedGroup = withinSegmentedGroup,
    )
}

@Composable
private fun RecruitCalcSwitchSetting(
    index: Int,
    itemCount: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showTime: Boolean = false,
    timeEditable: Boolean = false,
    totalMinutes: Int = 540,
    onTimeChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    withinSegmentedGroup: Boolean = false,
) {
    val normalizedMinutes = totalMinutes.coerceIn(60, 540)
    val hour = normalizedMinutes / 60
    val minute = normalizedMinutes % 60
    var durationExpanded by remember(checked, showTime, timeEditable) { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) MaaDesignTokens.Spacing.xs else 2.dp),
    ) {
        if (compact) {
            RecruitCompactSwitchSetting(
                title = title,
                checked = checked,
                onCheckedChange = onCheckedChange,
                totalMinutes = normalizedMinutes,
                showTime = showTime,
                timeEditable = timeEditable,
                editing = durationExpanded,
                onEditingChange = { durationExpanded = it },
            )
        } else if (withinSegmentedGroup) {
            RecruitConfirmationSettingRow(
                title = title,
                checked = checked,
                onCheckedChange = onCheckedChange,
                totalMinutes = normalizedMinutes,
                showTime = showTime,
                timeEditable = timeEditable,
                editing = durationExpanded,
                onEditingChange = { durationExpanded = it },
            )
        } else {
            val shape = animatedSegmentedItemShape(
                index = index,
                itemCount = itemCount,
                expanded = checked && timeEditable && durationExpanded,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                CompositionLocalProvider(LocalSettingItemShape provides shape) {
                    RecruitConfirmationSettingRow(
                        title = title,
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        totalMinutes = normalizedMinutes,
                        showTime = showTime,
                        timeEditable = timeEditable,
                        editing = durationExpanded,
                        onEditingChange = { durationExpanded = it },
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = checked && timeEditable && durationExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            SegmentedSettingsGroup {
                item {
                    NumberStepperSettingRow(
                        title = stringResource(R.string.panel_recruit_duration_hours),
                        value = hour,
                        range = 1..9,
                        icon = null,
                        onValueChange = { nextHour ->
                            onTimeChange(nextHour * 60 + if (nextHour == 9) 0 else minute)
                        },
                    )
                }
                item {
                    NumberStepperSettingRow(
                        title = stringResource(R.string.panel_recruit_duration_minutes),
                        value = minute,
                        range = 0..55,
                        step = 5,
                        enabled = hour < 9,
                        icon = null,
                        onValueChange = { nextMinute ->
                            onTimeChange(hour * 60 + nextMinute)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecruitCompactSwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    totalMinutes: Int,
    showTime: Boolean,
    timeEditable: Boolean,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = MaaDesignTokens.ListItem.minimumTouchTarget),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(
            modifier = Modifier.padding(MaaDesignTokens.Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                ExpressiveSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            }
            if (checked && showTime) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                ) {
                    RecruitTimeBadge(totalMinutes = totalMinutes)
                    if (timeEditable) {
                        IconButton(
                            onClick = { onEditingChange(!editing) },
                            modifier = Modifier.sizeIn(
                                minWidth = MaaDesignTokens.ListItem.minimumTouchTarget,
                                minHeight = MaaDesignTokens.ListItem.minimumTouchTarget,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(
                                    R.string.panel_recruit_edit_confirm_time,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecruitResultItem(result: RecruitCalcResult) {
    val levelColor = when {
        result.level >= 6 -> MaterialTheme.colorScheme.error
        result.level >= 5 -> MaterialTheme.colorScheme.tertiary
        result.level >= 4 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val containerColor = when {
        result.level >= 6 -> MaterialTheme.colorScheme.errorContainer
        result.level >= 5 -> MaterialTheme.colorScheme.tertiaryContainer
        result.level >= 4 -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    InfoCard(containerColor = containerColor) {
        Row(
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = "${result.level}★",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    modifier = Modifier.padding(
                        horizontal = MaaDesignTokens.Spacing.sm,
                        vertical = MaaDesignTokens.Spacing.xs,
                    ),
                )
            }

            Spacer(Modifier.width(MaaDesignTokens.Spacing.sm))

            Column(verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                ) {
                    result.tags.forEach { tag ->
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(
                                    horizontal = MaaDesignTokens.Spacing.sm,
                                    vertical = MaaDesignTokens.Spacing.xs,
                                ),
                            )
                        }
                    }
                }
                if (result.operators.isNotEmpty()) {
                    Text(
                        text = result.operators.joinToString("  ") { it.name },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
