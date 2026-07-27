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
import com.aliothmoon.maameow.domain.enums.RoguelikeBoskySubNodeType
import com.aliothmoon.maameow.domain.enums.RoguelikeMode
import com.aliothmoon.maameow.domain.enums.UiUsageConstants.Roguelike as RoguelikeUi
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.ITextField
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsScope

@Composable
fun ModeSpecificSettings(
    config: RoguelikeConfig,
    onConfigChange: (RoguelikeConfig) -> Unit,
    leadingItems: SegmentedSettingsScope.() -> Unit = {},
) {
    val collectibleAwardOptions = if (config.mode == RoguelikeMode.Collectible) {
        localizedRoguelikeCollectibleAwardOptions(config.theme)
    } else {
        emptyList()
    }
    SegmentedSettingsGroup {
        leadingItems()
        when (config.mode) {
            RoguelikeMode.Exp -> {
                if (config.theme != "Phantom") {
                    item {
                        CheckBoxWithLabel(
                            checked = config.stopAtFinalBoss,
                            onCheckedChange = { onConfigChange(config.copy(stopAtFinalBoss = it)) },
                            label = stringResource(R.string.panel_roguelike_stop_before_boss),
                        )
                    }
                }
                item {
                    CheckBoxWithLabel(
                        checked = config.stopAtMaxLevel,
                        onCheckedChange = { onConfigChange(config.copy(stopAtMaxLevel = it)) },
                        label = stringResource(R.string.panel_roguelike_stop_at_max_level),
                    )
                }
            }

            RoguelikeMode.Collectible -> collectibleItems(
                config,
                onConfigChange,
                collectibleAwardOptions,
            )

            RoguelikeMode.Squad -> {
                item {
                    CheckBoxWithLabel(
                        checked = config.monthlySquadAutoIterate,
                        onCheckedChange = {
                            onConfigChange(config.copy(monthlySquadAutoIterate = it))
                        },
                        label = stringResource(R.string.panel_roguelike_monthly_squad_auto_iterate),
                    )
                }
                if (config.monthlySquadAutoIterate) {
                    item {
                        CheckBoxWithLabel(
                            checked = config.monthlySquadCheckComms,
                            onCheckedChange = {
                                onConfigChange(config.copy(monthlySquadCheckComms = it))
                            },
                            label = stringResource(R.string.panel_roguelike_monthly_squad_comms),
                        )
                    }
                }
            }

            RoguelikeMode.Exploration -> {
                item {
                    CheckBoxWithLabel(
                        checked = config.deepExplorationAutoIterate,
                        onCheckedChange = {
                            onConfigChange(config.copy(deepExplorationAutoIterate = it))
                        },
                        label = stringResource(R.string.panel_roguelike_exploration_auto_iterate),
                    )
                }
            }

            RoguelikeMode.CLP_PDS -> {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ITextField(
                            value = config.expectedCollapsalParadigms,
                            onValueChange = {
                                onConfigChange(config.copy(expectedCollapsalParadigms = it))
                            },
                            label = stringResource(R.string.panel_roguelike_collapse_list),
                            placeholder = stringResource(R.string.panel_roguelike_collapse_list_placeholder),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            RoguelikeMode.FindPlaytime -> if (config.theme == "JieGarden") {
                item {
                    RoguelikeButtonGroup(
                        label = stringResource(R.string.panel_roguelike_playtime_target),
                        selectedValue = config.findPlaytimeTarget.name,
                        options = localizedRoguelikePlaytimeTargetOptions(),
                        onValueChange = {
                            onConfigChange(
                                config.copy(
                                    findPlaytimeTarget = RoguelikeBoskySubNodeType.valueOf(it),
                                ),
                            )
                        },
                    )
                }
            }

            RoguelikeMode.Investment -> Unit
        }
        themeSpecificItems(config, onConfigChange)
    }
}

private fun SegmentedSettingsScope.collectibleItems(
    config: RoguelikeConfig,
    onConfigChange: (RoguelikeConfig) -> Unit,
    awardOptions: List<Pair<String, String>>,
) {
    val professional = RoguelikeUi.isSquadProfessional(config.squad, config.mode, config.theme)
    val showEliteTwo = professional && config.theme in listOf("Mizuki", "Sami")
    val onlyEliteTwo = config.onlyStartWithEliteTwo && config.startWithEliteTwo && professional

    item {
        RoguelikeSquadButtonGroup(
            label = stringResource(R.string.panel_roguelike_collectible_squad),
            selectedValue = config.collectibleModeSquad,
            theme = config.theme,
            mode = config.mode,
            onValueChange = { onConfigChange(config.copy(collectibleModeSquad = it)) },
        )
    }
    item {
        CheckBoxWithLabel(
            checked = config.collectibleModeShopping,
            onCheckedChange = { onConfigChange(config.copy(collectibleModeShopping = it)) },
            label = stringResource(R.string.panel_roguelike_collectible_shopping),
        )
    }
    if (showEliteTwo) {
        item {
            CheckBoxWithLabel(
                checked = config.startWithEliteTwo,
                onCheckedChange = { checked ->
                    val updated = config.copy(
                        startWithEliteTwo = checked,
                        useSupport = if (checked) false else config.useSupport,
                        onlyStartWithEliteTwo = if (checked) config.onlyStartWithEliteTwo else false,
                    )
                    onConfigChange(updated)
                },
                label = stringResource(R.string.panel_roguelike_start_with_elite_two),
            )
        }
        if (config.startWithEliteTwo) {
            item {
                CheckBoxWithLabel(
                    checked = config.onlyStartWithEliteTwo,
                    onCheckedChange = {
                        onConfigChange(config.copy(onlyStartWithEliteTwo = it))
                    },
                    label = stringResource(R.string.panel_roguelike_only_start_with_elite_two),
                )
            }
        }
    }
    if (!onlyEliteTwo) {
        awardOptions.forEach { (key, display) ->
            item {
                CheckBoxWithLabel(
                    checked = key in config.collectibleStartAwards,
                    onCheckedChange = { checked ->
                        val awards = if (checked) {
                            config.collectibleStartAwards + key
                        } else {
                            config.collectibleStartAwards - key
                        }
                        onConfigChange(config.copy(collectibleStartAwards = awards))
                    },
                    label = display,
                )
            }
        }
    }
}
