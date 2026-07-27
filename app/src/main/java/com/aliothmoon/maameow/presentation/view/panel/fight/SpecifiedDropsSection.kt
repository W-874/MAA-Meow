package com.aliothmoon.maameow.presentation.view.panel.fight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.resource.ItemInfo
import com.aliothmoon.maameow.domain.enums.UiUsageConstants
import com.aliothmoon.maameow.presentation.components.CheckBoxWithExpandableTip
import com.aliothmoon.maameow.presentation.components.NumberStepperSettingRow
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup

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
                NumberStepperSettingRow(
                    title = stringResource(R.string.panel_fight_target_count),
                    value = config.dropsQuantity,
                    range = 1..999,
                    icon = null,
                    onValueChange = { onConfigChange(config.copy(dropsQuantity = it)) },
                )
            }
        }
    }
}
