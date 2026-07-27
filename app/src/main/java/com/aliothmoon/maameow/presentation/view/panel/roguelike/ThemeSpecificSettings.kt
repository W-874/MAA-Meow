package com.aliothmoon.maameow.presentation.view.panel.roguelike

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.RoguelikeConfig
import com.aliothmoon.maameow.domain.enums.RoguelikeMode
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.ITextField
import com.aliothmoon.maameow.presentation.components.SectionHeader
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsScope
import com.aliothmoon.maameow.domain.enums.UiUsageConstants.Roguelike as RoguelikeUi

@Composable
fun ThemeSpecificSettings(
    config: RoguelikeConfig,
    onConfigChange: (RoguelikeConfig) -> Unit,
    showSectionHeader: Boolean = true,
) {
    if (showSectionHeader) {
        val title = when (config.theme) {
            "Mizuki" -> R.string.panel_roguelike_mizuki_settings
            "Sami" -> R.string.panel_roguelike_sami_settings
            "JieGarden" -> R.string.panel_roguelike_jiegarden_settings
            else -> null
        }
        title?.let { SectionHeader(stringResource(it)) }
    }
    SegmentedSettingsGroup {
        themeSpecificItems(config, onConfigChange)
    }
}

internal fun SegmentedSettingsScope.themeSpecificItems(
    config: RoguelikeConfig,
    onConfigChange: (RoguelikeConfig) -> Unit,
) {
    when (config.theme) {
        "Mizuki" -> {
            item {
                CheckBoxWithLabel(
                    checked = config.refreshTraderWithDice,
                    onCheckedChange = {
                        onConfigChange(config.copy(refreshTraderWithDice = it))
                    },
                    label = stringResource(R.string.panel_roguelike_refresh_trader_with_dice),
                )
            }
        }

        "Sami" -> {
            val squadIsFoldartal = RoguelikeUi.isSquadFoldartal(
                config.squad,
                config.mode,
                config.theme,
            )
            val isCollectibleAvailable = config.mode == RoguelikeMode.Collectible

            if (squadIsFoldartal || isCollectibleAvailable) {
                if (isCollectibleAvailable) {
                    item {
                        CheckBoxWithLabel(
                            checked = config.firstFloorFoldartal,
                            onCheckedChange = {
                                onConfigChange(config.copy(firstFloorFoldartal = it))
                            },
                            label = stringResource(R.string.panel_roguelike_first_floor_foldartal),
                        )
                    }
                    if (config.firstFloorFoldartal) {
                        item {
                            PaddedTextField {
                                ITextField(
                                    value = config.firstFloorFoldartals,
                                    onValueChange = {
                                        onConfigChange(config.copy(firstFloorFoldartals = it))
                                    },
                                    placeholder = stringResource(R.string.panel_roguelike_foldartal_placeholder),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                if (squadIsFoldartal) {
                    item {
                        CheckBoxWithLabel(
                            checked = config.newSquad2StartingFoldartal,
                            onCheckedChange = {
                                onConfigChange(config.copy(newSquad2StartingFoldartal = it))
                            },
                            label = stringResource(R.string.panel_roguelike_new_squad_foldartal),
                        )
                    }
                    if (config.newSquad2StartingFoldartal) {
                        item {
                            PaddedTextField {
                                ITextField(
                                    value = config.newSquad2StartingFoldartals,
                                    onValueChange = {
                                        onConfigChange(config.copy(newSquad2StartingFoldartals = it))
                                    },
                                    placeholder = stringResource(R.string.panel_roguelike_new_squad_foldartal_placeholder),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }

        "JieGarden" -> {
            item {
                CheckBoxWithLabel(
                    checked = config.startWithSeed,
                    onCheckedChange = { onConfigChange(config.copy(startWithSeed = it)) },
                    label = stringResource(R.string.panel_roguelike_start_with_seed),
                )
            }
            if (config.startWithSeed) {
                item {
                    PaddedTextField {
                        ITextField(
                            value = config.seed,
                            onValueChange = { onConfigChange(config.copy(seed = it)) },
                            placeholder = stringResource(R.string.panel_roguelike_seed_placeholder),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaddedTextField(content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(16.dp), content = { content() })
}
