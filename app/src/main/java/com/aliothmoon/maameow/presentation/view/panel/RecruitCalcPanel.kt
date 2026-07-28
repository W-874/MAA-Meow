package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
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
import com.aliothmoon.maameow.presentation.components.INumericField
import com.aliothmoon.maameow.presentation.components.LocalSettingItemShape
import com.aliothmoon.maameow.presentation.components.animatedSegmentedItemShape
import com.aliothmoon.maameow.presentation.components.NumberStepperSettingRow
import com.aliothmoon.maameow.presentation.components.RecruitConfirmationSettingRow
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.navigation.LocalMainBottomBarPadding
import com.aliothmoon.maameow.presentation.viewmodel.ToolboxViewModel
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
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 2.dp,
            bottom = mainBottomBarPadding + 4.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RecruitCalcSwitchSetting(
                    index = 0,
                    itemCount = 5,
                    title = stringResource(R.string.panel_recruit_calc_auto_time),
                    checked = config.autoSetTime,
                    onCheckedChange = {
                        viewModel.onRecruitConfigChange(config.copy(autoSetTime = it))
                    },
                )
                listOf(3, 4, 5, 6).forEachIndexed { index, level ->
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
                        index = index + 1,
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
                            viewModel.onRecruitConfigChange(nextConfig)
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
                            viewModel.onRecruitConfigChange(nextConfig)
                        },
                    )
                }
            }
        }

        // 检测到的标签
        if (tags.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.panel_recruit_calc_detected_tags),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
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

        // 分隔线（有标签或有结果时显示）
        if (tags.isNotEmpty() || results.isNotEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
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
                Text(
                    text = resolvedStatusMessage.ifBlank {
                        stringResource(R.string.panel_recruit_calc_empty_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
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
) {
    val normalizedMinutes = totalMinutes.coerceIn(60, 540)
    val hour = normalizedMinutes / 60
    val minute = normalizedMinutes % 60
    var durationExpanded by remember(checked, showTime, timeEditable) { mutableStateOf(false) }
    val shape = animatedSegmentedItemShape(
        index = index,
        itemCount = itemCount,
        expanded = checked && timeEditable && durationExpanded,
    )

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        AnimatedVisibility(
            visible = checked && timeEditable && durationExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            SegmentedSettingsGroup(modifier = Modifier.padding(start = 12.dp)) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecruitResultItem(result: RecruitCalcResult) {
    val levelColor = when {
        result.level >= 6 -> Color(0xFFFF6B35)
        result.level >= 5 -> Color(0xFFFFD700)
        result.level >= 4 -> Color(0xFF9C7CFF)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bgColor = when {
        result.level >= 6 -> Color(0xFFFF6B35).copy(alpha = 0.08f)
        result.level >= 5 -> Color(0xFFFFD700).copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // 左侧星级徽标
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = levelColor.copy(alpha = 0.15f),
            ) {
                Text(
                    text = "${result.level}★",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // 右侧内容
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 标签组合
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    result.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                // 干员列表
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
