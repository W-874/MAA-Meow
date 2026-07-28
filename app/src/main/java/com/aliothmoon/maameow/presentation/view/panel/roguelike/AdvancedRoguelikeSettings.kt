package com.aliothmoon.maameow.presentation.view.panel.roguelike

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.RoguelikeConfig
import com.aliothmoon.maameow.domain.enums.RoguelikeMode
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.NumberStepperSettingRow
import com.aliothmoon.maameow.presentation.components.SectionHeader
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup

@Composable
fun AdvancedRoguelikeSettings(
    config: RoguelikeConfig,
    onConfigChange: (RoguelikeConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(stringResource(R.string.panel_roguelike_investment_settings))
        SegmentedSettingsGroup {
            item {
                CheckBoxWithLabel(
                    checked = config.investmentEnabled,
                    onCheckedChange = { onConfigChange(config.copy(investmentEnabled = it)) },
                    label = stringResource(R.string.panel_roguelike_investment_enabled),
                    enabled = config.mode != RoguelikeMode.Investment,
                )
            }
            if (config.investmentEnabled) {
                item {
                    NumberStepperSettingRow(
                        title = stringResource(R.string.panel_roguelike_invest_count),
                        value = config.investCount,
                        range = 0..999,
                        icon = null,
                        onValueChange = { onConfigChange(config.copy(investCount = it)) },
                    )
                }
                if (config.mode != RoguelikeMode.Collectible) {
                    item {
                        CheckBoxWithLabel(
                            checked = config.stopWhenInvestmentFull,
                            onCheckedChange = {
                                onConfigChange(config.copy(stopWhenInvestmentFull = it))
                            },
                            label = stringResource(R.string.panel_roguelike_stop_when_invest_full),
                        )
                    }
                }
                if (config.mode == RoguelikeMode.Investment) {
                    item {
                        CheckBoxWithLabel(
                            checked = config.investmentWithMoreScore,
                            onCheckedChange = {
                                onConfigChange(config.copy(investmentWithMoreScore = it))
                            },
                            label = stringResource(R.string.panel_roguelike_investment_more_score),
                        )
                    }
                }
            }
        }

        SectionHeader(stringResource(R.string.settings_section_other))
        ModeSpecificSettings(
            config = config,
            onConfigChange = onConfigChange,
        ) {
            item {
                NumberStepperSettingRow(
                    title = stringResource(R.string.panel_roguelike_starts_count),
                    value = config.startsCount,
                    range = 0..99999,
                    icon = null,
                    onValueChange = { onConfigChange(config.copy(startsCount = it)) },
                )
            }
        }
    }
}
