package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.RecruitConfig
import com.aliothmoon.maameow.data.resource.ResourceDataManager
import com.aliothmoon.maameow.presentation.components.INumericField
import com.aliothmoon.maameow.presentation.components.ExpressiveSwitch
import com.aliothmoon.maameow.presentation.components.SettingDropdown
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.components.NumberStepperSettingRow
import com.aliothmoon.maameow.presentation.components.RecruitTimeSelector
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipContent
import com.aliothmoon.maameow.presentation.components.tip.ExpandableTipIcon
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 自动公招配置面板
 *
 *
 * WPF源文件: RecruitSettingsUserControl.xaml
 * WPF ViewModel: RecruitSettingsUserControlModel.cs
 */
@Composable
fun RecruitConfigPanel(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_general)) }
        item {
            SegmentedSettingsGroup {
                item { UseExpeditedSection(config, onConfigChange) }
                item { RecruitMaxTimesSection(config, onConfigChange) }
            }
        }
        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_advanced)) }
        item {
            SegmentedSettingsGroup {
                item { SelectExtraTagsSection(config, onConfigChange) }
                item { AutoRecruitFirstListSection(config, onConfigChange) }
                item { RefreshLevel3Section(config, onConfigChange) }
                item { ForceRefreshSection(config, onConfigChange) }
                item { PreserveTagSection(config, onConfigChange) }
                item { ChooseLevel3Section(config, onConfigChange) }
                item { ChooseLevel4Section(config, onConfigChange) }
                item { ChooseLevel5Section(config, onConfigChange) }
            }
        }
    }
}

@Composable
private fun RecruitSwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    enabled: Boolean = true,
) {
    SettingRow(
        title = title,
        description = description,
        icon = null,
        enabled = enabled,
        onClick = { if (enabled) onCheckedChange(!checked) },
        trailing = {
            ExpressiveSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}


@Composable
private fun UseExpeditedSection(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit
) {
    RecruitSwitchSetting(
        title = stringResource(R.string.panel_recruit_use_expedited),
        description = stringResource(R.string.panel_recruit_use_expedited_tip),
        checked = config.useExpedited,
        onCheckedChange = { onConfigChange(config.copy(useExpedited = it)) },
    )
}


@Composable
private fun RecruitMaxTimesSection(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit
) {
    NumberStepperSettingRow(
        title = stringResource(R.string.panel_recruit_max_times_title),
        description = stringResource(R.string.panel_recruit_max_times_desc),
        value = config.maxRecruitTimes,
        range = 0..100,
        onValueChange = { onConfigChange(config.copy(maxRecruitTimes = it)) },
        icon = null,
    )
}


/**
 * 自动公招选择策略
 * WPF: ComboBox with AutoRecruitSelectExtraTagsList
 * 改用 RadioButton 单选按钮组
 */
@Composable
private fun SelectExtraTagsSection(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit
) {
    // 选项列表（AutoRecruitSelectExtraTagsList）
    val options = listOf(
        "0" to stringResource(R.string.panel_recruit_extra_tags_none),
        "1" to stringResource(R.string.panel_recruit_extra_tags_select),
        "2" to stringResource(R.string.panel_recruit_extra_tags_rare_only)
    )

    SettingDropdown(
        title = stringResource(R.string.panel_recruit_extra_tags_strategy),
        selected = config.selectExtraTags,
        options = options.map { it.first },
        optionLabel = { value -> options.firstOrNull { it.first == value }?.second ?: value },
        onSelected = { onConfigChange(config.copy(selectExtraTags = it)) },
        icon = null,
    )
}

/**
 * 3星Tag倾向（多选）
 */
@Composable
private fun AutoRecruitFirstListSection(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit,
    resourceDataManager: ResourceDataManager = koinInject()
) {
    val recruitTags by resourceDataManager.recruitTags.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var tipExpanded by remember { mutableStateOf(false) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.panel_recruit_level3_preference),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            ExpandableTipIcon(
                expanded = tipExpanded,
                onExpandedChange = { tipExpanded = it }
            )
        }

        ExpandableTipContent(
            visible = tipExpanded,
            tipText = stringResource(R.string.panel_recruit_level3_preference_tip)
        )

        // 多选标签面板 - 使用 FlowRow 自动换行布局
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ResourceDataManager.AUTO_RECRUIT_TAG_KEYS.mapNotNull { key ->
                        recruitTags[key]
                    }.forEach { (display, client) ->
                        val isSelected = config.autoRecruitFirstList.contains(client)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newList = if (isSelected) {
                                    config.autoRecruitFirstList - client
                                } else {
                                    config.autoRecruitFirstList + client
                                }
                                onConfigChange(config.copy(autoRecruitFirstList = newList))
                            },
                            label = {
                                Text(
                                    text = display,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = null,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // 已选择计数
                if (config.autoRecruitFirstList.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.panel_recruit_selected_count, config.autoRecruitFirstList.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
        }
    }
}

/**
 * 刷新三星Tags
 * WPF: CheckBox with RefreshLevel3 binding
 */
@Composable
private fun RefreshLevel3Section(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit
) = RecruitSwitchSetting(
    title = stringResource(R.string.panel_recruit_refresh_level3),
    checked = config.refreshLevel3,
    onCheckedChange = { onConfigChange(config.copy(refreshLevel3 = it)) },
)

/**
 * 无招聘许可时继续尝试刷新Tags（依赖RefreshLevel3）
 * WPF: CheckBox with ForceRefresh binding, enabled by RefreshLevel3
 */
@Composable
private fun ForceRefreshSection(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit
) = RecruitSwitchSetting(
    title = stringResource(R.string.panel_recruit_force_refresh),
    checked = config.forceRefresh,
    enabled = config.refreshLevel3,
    onCheckedChange = { onConfigChange(config.copy(forceRefresh = it)) },
)

/**
 * 保留指定词条
 * 对应 WPF: PreserveTagEnabled / PreserveTagList (#16586)
 *
 * 注意 name-space：数据源同为 recruitTags，但本节存储的是「中文规范名」(map 的 key)，
 * 而非 AutoRecruitFirstListSection 的「客户端名」(pair.second / client)。
 * 原因：core 对 preserve_tags 按中文 TagId 直接比较（find_first_of(tag_ids, preserve_tags)），
 * first_tags 才是按本地化名做子串匹配。对齐 WPF：
 *   - _autoRecruitTagShowList(first)   → Value = tag.Client
 *   - _autoRecruitSkipTagShowList(skip) → Value = tag.Key
 * 默认值 "支援机械" 即中文规范 key（WPF LegacyRobotTag），非中文客户端同样生效。
 * 启用后使用 preserve_tags 参数，不再输出 skip_robot。
 */
@Composable
private fun PreserveTagSection(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit,
    resourceDataManager: ResourceDataManager = koinInject()
) {
    val recruitTags by resourceDataManager.recruitTags.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        RecruitSwitchSetting(
            title = stringResource(R.string.panel_recruit_preserve_tag_enabled),
            description = stringResource(R.string.panel_recruit_preserve_tag_enabled_tip),
            checked = config.preserveTagEnabled,
            onCheckedChange = { onConfigChange(config.copy(preserveTagEnabled = it)) },
        )

        if (config.preserveTagEnabled) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recruitTags.forEach { (tagKey, pair) ->
                            val display = pair.first
                            // 存中文规范名 tagKey（map 的 key），对齐 core preserve_tags 与 WPF
                            val isSelected = config.preserveTagList.contains(tagKey)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newList = if (isSelected) {
                                        config.preserveTagList - tagKey
                                    } else {
                                        config.preserveTagList + tagKey
                                    }
                                    onConfigChange(config.copy(preserveTagList = newList))
                                },
                                label = {
                                    Text(
                                        text = display,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    if (config.preserveTagList.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.panel_recruit_selected_count, config.preserveTagList.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
            }
        }
    }
}

/**
 * 自动选择三星 + 时长设置
 * WPF: CheckBox + 两个NumericUpDown (Hour + Min)
 */
@Composable
private fun ChooseLevel3Section(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecruitSwitchSetting(
            title = stringResource(R.string.panel_recruit_choose_level3),
            checked = config.chooseLevel3,
            onCheckedChange = { onConfigChange(config.copy(chooseLevel3 = it)) },
        )

        // 时长选择器
        RecruitTimeSelector(
            enabled = config.chooseLevel3,
            totalMinutes = config.chooseLevel3Hour * 60 + config.chooseLevel3Min,
            onTimeChange = { total ->
                onConfigChange(config.copy(
                    chooseLevel3Hour = total / 60,
                    chooseLevel3Min = total % 60
                ))
            }
        )
    }
}

/**
 * 自动选择四星 + 时长设置
 * WPF: CheckBox + 两个NumericUpDown (Hour + Min)
 */
@Composable
private fun ChooseLevel4Section(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecruitSwitchSetting(
            title = stringResource(R.string.panel_recruit_choose_level4),
            checked = config.chooseLevel4,
            onCheckedChange = { onConfigChange(config.copy(chooseLevel4 = it)) },
        )

        RecruitTimeSelector(
            enabled = config.chooseLevel4,
            totalMinutes = config.chooseLevel4Hour * 60 + config.chooseLevel4Min,
            onTimeChange = { total ->
                onConfigChange(config.copy(
                    chooseLevel4Hour = total / 60,
                    chooseLevel4Min = total % 60
                ))
            }
        )
    }
}

/**
 * 自动选择五星 + 时长设置
 * WPF: CheckBox + 两个NumericUpDown (Hour + Min)
 */
@Composable
private fun ChooseLevel5Section(
    config: RecruitConfig,
    onConfigChange: (RecruitConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecruitSwitchSetting(
            title = stringResource(R.string.panel_recruit_choose_level5),
            checked = config.chooseLevel5,
            onCheckedChange = { onConfigChange(config.copy(chooseLevel5 = it)) },
        )

        // 对齐上游 v6.13.0-beta.1：5 星时间锁死 9:00，不再允许编辑
        RecruitTimeSelector(
            enabled = false,
            totalMinutes = 540,
            onTimeChange = {}
        )
    }
}
