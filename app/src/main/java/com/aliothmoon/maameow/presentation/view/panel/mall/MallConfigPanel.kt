package com.aliothmoon.maameow.presentation.view.panel.mall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.MallConfig
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.data.resource.ActivityManager
import com.aliothmoon.maameow.domain.models.resolveMallCreditFightAvailability
import com.aliothmoon.maameow.presentation.components.CheckBoxWithExpandableTip
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingDropdown
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.presentation.components.SettingActionButton
import com.aliothmoon.maameow.presentation.components.ReorderableFlowRow
import com.aliothmoon.maameow.presentation.view.panel.TaskSettingsSectionTitle
import com.aliothmoon.maameow.presentation.view.panel.LocalTaskPanelBottomPadding
import org.koin.compose.koinInject

@Composable
fun MallConfigPanel(config: MallConfig, onConfigChange: (MallConfig) -> Unit) {
    var isDraggingItem by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            bottom = LocalTaskPanelBottomPadding.current,
        ),
        userScrollEnabled = !isDraggingItem,
    ) {
        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_general)) }
        item { BasicMallSettings(config, onConfigChange) }

        item { TaskSettingsSectionTitle(stringResource(R.string.panel_mall_priority_title)) }
        item {
            PriorityItemsSection(
                config = config,
                onConfigChange = onConfigChange,
                onDraggingChanged = { isDraggingItem = it },
            )
        }

        item { TaskSettingsSectionTitle(stringResource(R.string.panel_mall_blacklist_title)) }
        item {
            BlacklistSection(
                config = config,
                onConfigChange = onConfigChange,
                onDraggingChanged = { isDraggingItem = it },
            )
        }

        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_advanced)) }
        item {
            SegmentedSettingsGroup {
                item {
                    CheckBoxWithExpandableTip(
                        checked = config.forceShoppingIfCreditFull,
                        onCheckedChange = {
                            onConfigChange(config.copy(forceShoppingIfCreditFull = it))
                        },
                        label = stringResource(R.string.panel_mall_force_shopping_if_full),
                        tipText = stringResource(R.string.panel_mall_force_shopping_if_full_tip),
                        enabled = config.shopping,
                    )
                }
                item {
                    CheckBoxWithExpandableTip(
                        checked = config.onlyBuyDiscount,
                        onCheckedChange = { onConfigChange(config.copy(onlyBuyDiscount = it)) },
                        label = stringResource(R.string.panel_mall_only_discount),
                        tipText = stringResource(R.string.panel_mall_only_discount_tip),
                        enabled = config.shopping,
                    )
                }
                item {
                    CheckBoxWithExpandableTip(
                        checked = config.reserveMaxCredit,
                        onCheckedChange = { onConfigChange(config.copy(reserveMaxCredit = it)) },
                        label = stringResource(R.string.panel_mall_reserve_credit),
                        tipText = stringResource(R.string.panel_mall_reserve_credit_tip),
                        enabled = config.shopping,
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicMallSettings(config: MallConfig, onConfigChange: (MallConfig) -> Unit) {
    val taskChainState: TaskChainState = koinInject()
    val activityManager: ActivityManager = koinInject()
    val chain by taskChainState.chain.collectAsStateWithLifecycle()
    val creditFightAvailability = remember(chain, activityManager) {
        resolveMallCreditFightAvailability(chain, activityManager)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedSettingsGroup {
            item {
                CheckBoxWithLabel(
                    checked = config.visitFriends,
                    onCheckedChange = { onConfigChange(config.copy(visitFriends = it)) },
                    label = stringResource(R.string.panel_mall_visit_friends),
                )
            }
            item {
                CheckBoxWithExpandableTip(
                    checked = config.shopping,
                    onCheckedChange = { onConfigChange(config.copy(shopping = it)) },
                    label = stringResource(R.string.panel_mall_shopping),
                    tipText = stringResource(R.string.panel_mall_shopping_tip),
                )
            }
            item {
                CheckBoxWithExpandableTip(
                    checked = config.creditFight,
                    onCheckedChange = { onConfigChange(config.copy(creditFight = it)) },
                    label = stringResource(R.string.panel_mall_credit_fight),
                    tipText = stringResource(R.string.panel_mall_credit_fight_tip),
                )
            }
        }

        AnimatedVisibility(
            visible = config.creditFight,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentedSettingsGroup {
                    item {
                        FormationSelector(
                            selectedFormation = config.creditFightFormation,
                            onFormationChange = {
                                onConfigChange(config.copy(creditFightFormation = it))
                            },
                        )
                    }
                }
                MallNotice(text = stringResource(R.string.panel_mall_credit_fight_notice))
            }
        }

        if (config.creditFight && !creditFightAvailability.isAvailable) {
            MallNotice(
                text = creditFightAvailability.warningMessage
                    ?: stringResource(R.string.panel_mall_credit_fight_unavailable),
                warning = true,
            )
        }
    }
}

@Composable
private fun FormationSelector(selectedFormation: Int, onFormationChange: (Int) -> Unit) {
    SettingDropdown(
        title = stringResource(R.string.panel_mall_use_formation),
        selected = selectedFormation,
        options = MallConfig.FORMATION_OPTIONS.map { it.first },
        optionLabel = { value ->
            MallConfig.FORMATION_OPTIONS.firstOrNull { it.first == value }?.second.orEmpty()
        },
        onSelected = onFormationChange,
        icon = null,
        singleLine = true,
    )
}

@Composable
private fun MallNotice(text: String, warning: Boolean = false) {
    SegmentedSettingsGroup(
        containerColor = if (warning) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
    ) {
        item {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (warning) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun PriorityItemsSection(
    config: MallConfig,
    onConfigChange: (MallConfig) -> Unit,
    onDraggingChanged: (Boolean) -> Unit,
) {
    var showAddPanel by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.panel_mall_priority_reorder_tip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (!config.shopping) {
            MallNotice(
                text = stringResource(R.string.panel_mall_enable_shopping_first),
                warning = true,
            )
        }

        SegmentedSettingsGroup {
            item {
                MallItemTagList(
                    items = config.buyFirst,
                    enabled = config.shopping,
                    isBlacklist = false,
                    onItemsChanged = { onConfigChange(config.copy(buyFirst = it)) },
                    onDraggingChanged = onDraggingChanged,
                )
            }
            item {
                SettingActionButton(
                    title = if (showAddPanel) {
                        stringResource(R.string.common_collapse)
                    } else {
                        stringResource(R.string.panel_mall_add_item)
                    },
                    icon = Icons.Rounded.Add,
                    onClick = { showAddPanel = !showAddPanel },
                    enabled = config.shopping,
                )
            }
        }

        AnimatedVisibility(
            visible = showAddPanel,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            InlineAddItemPanel(
                onItemAdded = { newItem ->
                    if (newItem.isNotBlank() && newItem.trim() !in config.buyFirst) {
                        onConfigChange(config.copy(buyFirst = config.buyFirst + newItem.trim()))
                    }
                    showAddPanel = false
                },
                onCancel = { showAddPanel = false },
            )
        }
    }
}

@Composable
private fun BlacklistSection(
    config: MallConfig,
    onConfigChange: (MallConfig) -> Unit,
    onDraggingChanged: (Boolean) -> Unit,
) {
    var showAddPanel by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.panel_mall_blacklist_tip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (!config.shopping) {
            MallNotice(
                text = stringResource(R.string.panel_mall_enable_shopping_first),
                warning = true,
            )
        }

        SegmentedSettingsGroup {
            item {
                MallItemTagList(
                    items = config.blacklist,
                    enabled = config.shopping,
                    isBlacklist = true,
                    onItemsChanged = { onConfigChange(config.copy(blacklist = it)) },
                    onDraggingChanged = onDraggingChanged,
                )
            }
            item {
                SettingActionButton(
                    title = if (showAddPanel) {
                        stringResource(R.string.common_collapse)
                    } else {
                        stringResource(R.string.panel_mall_add_blacklist)
                    },
                    icon = Icons.Rounded.Add,
                    onClick = { showAddPanel = !showAddPanel },
                    enabled = config.shopping,
                )
            }
        }

        AnimatedVisibility(
            visible = showAddPanel,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            InlineBlacklistAddPanel(
                onItemAdded = { newItem ->
                    if (newItem.isNotBlank() && newItem.trim() !in config.blacklist) {
                        onConfigChange(config.copy(blacklist = config.blacklist + newItem.trim()))
                    }
                    showAddPanel = false
                },
                onCancel = { showAddPanel = false },
            )
        }
    }
}

@Composable
private fun MallItemTagList(
    items: List<String>,
    enabled: Boolean,
    isBlacklist: Boolean,
    onItemsChanged: (List<String>) -> Unit,
    onDraggingChanged: (Boolean) -> Unit,
) {
    if (items.isEmpty()) {
        Text(
            text = stringResource(
                if (isBlacklist) {
                    R.string.panel_mall_blacklist_empty
                } else {
                    R.string.panel_mall_priority_empty
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
        )
        return
    }

    ReorderableFlowRow(
        items = items,
        itemKey = { it },
        onOrderChanged = onItemsChanged,
        onDraggingChanged = onDraggingChanged,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    ) { itemName, _, dragModifier ->
        FilterChip(
            selected = true,
            enabled = enabled,
            onClick = { onItemsChanged(items - itemName) },
            label = {
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.common_delete),
                    modifier = Modifier.height(14.dp),
                )
            },
            colors = if (isBlacklist) {
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                    selectedTrailingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
                )
            },
            border = null,
            modifier = dragModifier.height(32.dp),
        )
    }
}
