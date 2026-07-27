package com.aliothmoon.maameow.presentation.view.panel.fight

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.NumberStepperSettingRow
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup

@Composable
fun MedicineAndStoneSection(
    config: FightConfig,
    onConfigChange: (FightConfig) -> Unit,
) {
    SegmentedSettingsGroup {
        item {
            CheckBoxWithLabel(
                checked = config.useMedicine,
                onCheckedChange = {
                    onConfigChange(
                        config.copy(
                            useMedicine = it,
                            useStone = if (!it) false else config.useStone,
                        ),
                    )
                },
                label = stringResource(R.string.panel_fight_use_medicine),
                enabled = !config.useStone,
            )
        }
        if (config.useMedicine) {
            item {
                NumberStepperSettingRow(
                    title = stringResource(R.string.panel_fight_use_medicine_count),
                    value = config.medicineNumber,
                    range = 0..999,
                    icon = null,
                    enabled = !config.useStone,
                    onValueChange = { onConfigChange(config.copy(medicineNumber = it)) },
                )
            }
        }
        item {
            CheckBoxWithLabel(
                checked = config.hasTimesLimited,
                onCheckedChange = { onConfigChange(config.copy(hasTimesLimited = it)) },
                label = stringResource(R.string.panel_fight_limit_times),
            )
        }
        if (config.hasTimesLimited) {
            item {
                NumberStepperSettingRow(
                    title = stringResource(R.string.panel_fight_stop_after_times),
                    value = config.maxTimes,
                    range = 0..999,
                    icon = null,
                    onValueChange = { onConfigChange(config.copy(maxTimes = it)) },
                )
            }
            if (config.series > 0 && config.maxTimes % config.series != 0) {
                item {
                    Text(
                        text = stringResource(
                            R.string.panel_fight_series_warning,
                            config.maxTimes,
                            config.series,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
