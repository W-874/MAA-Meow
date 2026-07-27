package com.aliothmoon.maameow.presentation.view.panel.fight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.resource.ItemInfo
import com.aliothmoon.maameow.domain.enums.UiUsageConstants
import com.aliothmoon.maameow.presentation.components.CheckBoxWithExpandableTip
import com.aliothmoon.maameow.presentation.components.NumberStepperSettingRow
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.view.panel.common.ItemButtonGroup

@Composable
fun SpecifiedDropsSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit,
    dropItems: List<ItemInfo>,
) {
    val itemNameMap = remember(dropItems) { dropItems.associate { it.id to it.name } }
    val itemIds = remember(dropItems) {
        dropItems.map { it.id }.ifEmpty { UiUsageConstants.dropItems }
    }

    SegmentedSettingsGroup {
        item {
            CheckBoxWithExpandableTip(
                checked = config.isSpecifiedDrops,
                onCheckedChange = {
                    onConfigChange(
                        config.copy(
                            isSpecifiedDrops = it,
                            dropsItemId = if (!it) "" else config.dropsItemId,
                            dropsQuantity = if (!it) 5 else config.dropsQuantity,
                            isInventoryTarget = if (!it) false else config.isInventoryTarget,
                        ),
                    )
                },
                label = stringResource(R.string.panel_fight_specified_drops),
                tipText = stringResource(R.string.panel_fight_specified_drops_tip),
            )
        }
        if (config.isSpecifiedDrops) {
            item {
                ItemButtonGroup(
                    label = stringResource(R.string.panel_fight_material),
                    selectedValue = config.dropsItemId,
                    items = itemIds,
                    onItemSelected = { onConfigChange(config.copy(dropsItemId = it)) },
                    displayMapper = { id -> itemNameMap[id] ?: id },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !config.isInventoryTarget,
                        onClick = { onConfigChange(config.copy(isInventoryTarget = false)) },
                        label = { Text(stringResource(R.string.panel_fight_drops_mode_quantity)) },
                    )
                    FilterChip(
                        selected = config.isInventoryTarget,
                        onClick = { onConfigChange(config.copy(isInventoryTarget = true)) },
                        label = {
                            Text(stringResource(R.string.panel_fight_drops_mode_target_inventory))
                        },
                    )
                }
            }
            item {
                NumberStepperSettingRow(
                    title = stringResource(
                        if (config.isInventoryTarget) {
                            R.string.panel_fight_target_inventory
                        } else {
                            R.string.panel_fight_target_count
                        },
                    ),
                    value = config.dropsQuantity,
                    range = if (config.isInventoryTarget) 1..1_145_141_919 else 1..999,
                    description = if (config.isInventoryTarget) {
                        stringResource(R.string.panel_fight_target_inventory_tip)
                    } else {
                        null
                    },
                    icon = null,
                    onValueChange = { onConfigChange(config.copy(dropsQuantity = it)) },
                )
            }
        }
    }
}
