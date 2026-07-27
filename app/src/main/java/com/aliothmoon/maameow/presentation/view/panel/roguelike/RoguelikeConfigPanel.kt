package com.aliothmoon.maameow.presentation.view.panel.roguelike

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.RoguelikeConfig
import com.aliothmoon.maameow.data.resource.ResourceDataManager
import com.aliothmoon.maameow.domain.enums.RoguelikeMode
import com.aliothmoon.maameow.domain.enums.UiUsageConstants.Roguelike as RoguelikeUi
import com.aliothmoon.maameow.presentation.components.CoreCharSelector
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.view.panel.TaskSettingsSectionTitle
import org.koin.compose.koinInject

@Composable
fun RoguelikeConfigPanel(
    config: RoguelikeConfig,
    onConfigChange: (RoguelikeConfig) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_general)) }
        item { BasicRoguelikeSettings(config, onConfigChange) }
        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_advanced)) }
        item { AdvancedRoguelikeSettings(config, onConfigChange) }
    }
}

@Composable
private fun BasicRoguelikeSettings(
    config: RoguelikeConfig,
    onConfigChange: (RoguelikeConfig) -> Unit,
    resourceDataManager: ResourceDataManager = koinInject(),
) {
    SegmentedSettingsGroup {
        item {
            RoguelikeButtonGroup(
                label = stringResource(R.string.panel_roguelike_theme),
                selectedValue = config.theme,
                options = localizedRoguelikeThemeOptions(),
                onValueChange = { newTheme ->
                    val newSquads = RoguelikeUi.getSquadOptionsForTheme(newTheme, config.mode)
                    val newSquad = config.squad.takeIf { it in newSquads }
                        ?: RoguelikeUi.DEFAULT_SQUAD
                    val newMode = config.mode.takeIf {
                        RoguelikeUi.isModeValidForTheme(it, newTheme)
                    } ?: RoguelikeMode.valueOf(
                        RoguelikeUi.getModeKeysForTheme(newTheme).firstOrNull() ?: "Exp",
                    )
                    val maxDifficulty = RoguelikeUi.getMaxDifficultyForTheme(newTheme)
                    val newDifficulty = config.difficulty.takeIf {
                        it in 0..maxDifficulty || it == -1 || it == Int.MAX_VALUE
                    } ?: -1
                    val roles = RoguelikeUi.getRoleKeysForTheme(newTheme)
                    val newRoles = config.roles.takeIf { it in roles } ?: RoguelikeUi.DEFAULT_ROLE
                    val collectibleSquad = config.collectibleModeSquad.takeIf { it in newSquads }
                        ?: newSquad
                    val validAwards = RoguelikeUi.getCollectibleAwardKeys(newTheme).toSet()

                    onConfigChange(
                        config.copy(
                            theme = newTheme,
                            squad = newSquad,
                            mode = newMode,
                            difficulty = newDifficulty,
                            roles = newRoles,
                            collectibleModeSquad = collectibleSquad,
                            collectibleStartAwards = config.collectibleStartAwards.intersect(validAwards),
                        ),
                    )
                },
            )
        }
        item {
            RoguelikeDifficultyButtonGroup(
                label = stringResource(R.string.panel_roguelike_difficulty),
                selectedValue = config.difficulty,
                theme = config.theme,
                onValueChange = { onConfigChange(config.copy(difficulty = it)) },
            )
        }
        item {
            Column {
                RoguelikeButtonGroup(
                    label = stringResource(R.string.panel_roguelike_strategy),
                    selectedValue = config.mode.name,
                    options = localizedRoguelikeModeOptionsForTheme(config.theme),
                    onValueChange = { value ->
                        val newMode = RoguelikeMode.valueOf(value)
                        var updated = config.copy(mode = newMode)
                        if (newMode == RoguelikeMode.Investment) {
                            updated = updated.copy(investmentEnabled = true)
                        }
                        val squads = RoguelikeUi.getSquadOptionsForTheme(config.theme, newMode)
                        val squad = updated.squad.takeIf { it in squads } ?: RoguelikeUi.DEFAULT_SQUAD
                        val collectibleSquad = updated.collectibleModeSquad.takeIf { it in squads }
                            ?: squad
                        onConfigChange(
                            updated.copy(
                                squad = squad,
                                collectibleModeSquad = collectibleSquad,
                            ),
                        )
                    },
                )
                localizedRoguelikeModeDescription(config.mode).takeIf { it.isNotEmpty() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    )
                }
            }
        }
        item {
            RoguelikeSquadButtonGroup(
                label = stringResource(R.string.panel_roguelike_squad),
                selectedValue = config.squad,
                theme = config.theme,
                mode = config.mode,
                onValueChange = { onConfigChange(config.copy(squad = it)) },
            )
        }
        item {
            RoguelikeButtonGroup(
                label = stringResource(R.string.panel_roguelike_roles),
                selectedValue = config.roles,
                options = localizedRoguelikeRoleOptionsForTheme(config.theme),
                onValueChange = { onConfigChange(config.copy(roles = it)) },
            )
        }
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                CoreCharSelector(
                    value = config.coreChar,
                    onValueChange = { onConfigChange(config.copy(coreChar = it)) },
                    theme = config.theme,
                    resourceDataManager = resourceDataManager,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
