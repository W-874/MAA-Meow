package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.ReclamationConfig
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.ITextField
import com.aliothmoon.maameow.presentation.components.NumberStepperSettingRow
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingDropdown
import com.aliothmoon.maameow.presentation.components.SettingRow

@Composable
fun ReclamationConfigPanel(
    config: ReclamationConfig,
    onConfigChange: (ReclamationConfig) -> Unit,
) {
    val sanitized = config.sanitizedMode()
    LaunchedEffect(config.theme, config.mode) {
        if (sanitized != config.mode) {
            onConfigChange(config.copy(mode = sanitized))
        }
    }

    val themeOptions = localizedReclamationThemeOptions()
    val talesModeOptions = localizedReclamationModeOptions()
    val anchorModeOptions = localizedRelaunchAnchorModeOptions()
    val incrementOptions = localizedReclamationIncrementModeOptions()
    val isRelaunchAnchor = config.theme == "RelaunchAnchor"
    val isArchiveMode = !isRelaunchAnchor &&
        config.mode == ReclamationConfig.MODE_PROSPERITY_IN_SAVE

    val guidanceTitle: String
    val guidanceText: String
    val guidanceWarning: Boolean
    when {
        isRelaunchAnchor -> {
            guidanceTitle = stringResource(R.string.panel_reclamation_stage_instructions)
            guidanceText = stringResource(
                when (config.mode) {
                    ReclamationConfig.MODE_RA15 -> R.string.panel_reclamation_relaunch_anchor_tip_ra15
                    ReclamationConfig.MODE_RA4 -> R.string.panel_reclamation_relaunch_anchor_tip_ra4
                    else -> R.string.panel_reclamation_relaunch_anchor_tip_ra1
                },
            )
            guidanceWarning = config.mode == ReclamationConfig.MODE_RA15
        }
        config.mode == ReclamationConfig.MODE_PROSPERITY_NO_SAVE -> {
            guidanceTitle = stringResource(R.string.panel_reclamation_no_save_title)
            guidanceText = listOf(
                stringResource(R.string.panel_reclamation_notice),
                stringResource(R.string.panel_reclamation_no_save_line1),
                stringResource(R.string.panel_reclamation_no_save_line2),
                stringResource(R.string.panel_reclamation_no_save_line3),
                stringResource(R.string.panel_reclamation_no_save_line4),
                stringResource(R.string.panel_reclamation_no_save_line5),
            ).joinToString("\n\n")
            guidanceWarning = true
        }
        else -> {
            guidanceTitle = stringResource(R.string.panel_reclamation_with_save_title)
            guidanceText = listOf(
                stringResource(R.string.panel_reclamation_archive_tip),
                stringResource(R.string.panel_reclamation_with_save_line1),
                stringResource(R.string.panel_reclamation_with_save_line2),
                stringResource(R.string.panel_reclamation_with_save_line3),
            ).joinToString("\n\n")
            guidanceWarning = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            bottom = LocalTaskPanelBottomPadding.current,
        ),
    ) {
        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_general)) }
        item {
            SegmentedSettingsGroup {
                item {
                    ReclamationButtonGroup(
                        label = stringResource(R.string.panel_reclamation_theme),
                        options = themeOptions,
                        selectedValue = config.theme,
                        onValueChange = {
                            val theme = it as String
                            onConfigChange(
                                if (theme == "RelaunchAnchor") {
                                    config.copy(
                                        theme = theme,
                                        mode = ReclamationConfig.MODE_RA1,
                                        clearStore = false,
                                    )
                                } else {
                                    config.copy(
                                        theme = theme,
                                        mode = ReclamationConfig.MODE_PROSPERITY_NO_SAVE,
                                    )
                                },
                            )
                        },
                    )
                }
                item {
                    ReclamationButtonGroup(
                        label = stringResource(
                            if (isRelaunchAnchor) {
                                R.string.panel_reclamation_relaunch_anchor_stage
                            } else {
                                R.string.panel_reclamation_strategy
                            },
                        ),
                        options = if (isRelaunchAnchor) anchorModeOptions else talesModeOptions,
                        selectedValue = config.mode,
                        onValueChange = { onConfigChange(config.copy(mode = it as Int)) },
                    )
                }
                if (!isRelaunchAnchor &&
                    config.mode == ReclamationConfig.MODE_PROSPERITY_NO_SAVE
                ) {
                    item {
                        CheckBoxWithLabel(
                            checked = config.clearStore,
                            onCheckedChange = {
                                onConfigChange(config.copy(clearStore = it))
                            },
                            label = stringResource(R.string.panel_reclamation_clear_store),
                        )
                    }
                }
            }
        }
        item {
            SegmentedSettingsGroup {
                item {
                    ReclamationGuidanceRow(
                        title = guidanceTitle,
                        text = guidanceText,
                        warning = guidanceWarning,
                    )
                }
            }
        }

        item { TaskSettingsSectionTitle(stringResource(R.string.common_tab_advanced)) }
        item {
            SegmentedSettingsGroup {
                item {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.panel_reclamation_tool_name),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isArchiveMode) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                        ITextField(
                            value = config.toolToCraft,
                            onValueChange = {
                                onConfigChange(config.copy(toolToCraft = it))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = stringResource(R.string.panel_reclamation_tool_placeholder),
                            singleLine = false,
                            enabled = isArchiveMode,
                        )
                        Text(
                            text = stringResource(R.string.panel_reclamation_tool_separator_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    ReclamationButtonGroup(
                        label = stringResource(R.string.panel_reclamation_increment_mode),
                        options = incrementOptions,
                        selectedValue = config.incrementMode,
                        onValueChange = {
                            onConfigChange(config.copy(incrementMode = it as Int))
                        },
                        enabled = isArchiveMode,
                    )
                }
                item {
                    NumberStepperSettingRow(
                        title = stringResource(R.string.panel_reclamation_max_craft_count),
                        value = config.maxCraftCountPerRound,
                        onValueChange = {
                            onConfigChange(config.copy(maxCraftCountPerRound = it))
                        },
                        range = 1..99,
                        icon = null,
                        enabled = isArchiveMode,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReclamationGuidanceRow(
    title: String,
    text: String,
    warning: Boolean,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Column {
        SettingRow(
            title = title,
            description = stringResource(
                if (expanded) R.string.common_collapse else R.string.common_expand,
            ),
            titleColor = if (warning) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            icon = null,
            onClick = { expanded = !expanded },
        )
        AnimatedVisibility(visible = expanded) {
            Text(
                text = text,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (warning) FontWeight.Medium else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun ReclamationButtonGroup(
    label: String,
    options: List<Pair<Any, String>>,
    selectedValue: Any,
    onValueChange: (Any) -> Unit,
    enabled: Boolean = true,
) {
    SettingDropdown(
        title = label,
        selected = selectedValue,
        options = options.map { it.first },
        optionLabel = { value ->
            options.firstOrNull { it.first == value }?.second ?: value.toString()
        },
        onSelected = onValueChange,
        enabled = enabled,
        icon = null,
    )
}

@Composable
private fun localizedReclamationThemeOptions(): List<Pair<Any, String>> {
    return ReclamationConfig.THEME_KEYS.map { theme ->
        theme to localizedReclamationThemeLabel(theme)
    }
}

@Composable
internal fun localizedReclamationThemeLabel(theme: String): String = when (theme) {
    "Tales" -> stringResource(R.string.panel_reclamation_theme_tales)
    "Fire" -> stringResource(R.string.panel_reclamation_theme_fire)
    "RelaunchAnchor" -> stringResource(R.string.panel_reclamation_theme_relaunch_anchor)
    else -> theme
}

@Composable
private fun localizedReclamationModeOptions(): List<Pair<Any, String>> {
    return ReclamationConfig.TALES_MODE_VALUES.map { mode ->
        mode to when (mode) {
            ReclamationConfig.MODE_PROSPERITY_NO_SAVE -> stringResource(R.string.panel_reclamation_mode_no_save)
            ReclamationConfig.MODE_PROSPERITY_IN_SAVE -> stringResource(R.string.panel_reclamation_mode_with_save)
            else -> mode.toString()
        }
    }
}

@Composable
private fun localizedRelaunchAnchorModeOptions(): List<Pair<Any, String>> {
    return ReclamationConfig.RELAUNCH_ANCHOR_MODE_VALUES.map { mode ->
        mode to when (mode) {
            ReclamationConfig.MODE_RA1 -> stringResource(R.string.panel_reclamation_mode_ra1)
            ReclamationConfig.MODE_RA4 -> stringResource(R.string.panel_reclamation_mode_ra4)
            ReclamationConfig.MODE_RA15 -> stringResource(R.string.panel_reclamation_mode_ra15)
            else -> mode.toString()
        }
    }
}

@Composable
private fun localizedReclamationIncrementModeOptions(): List<Pair<Any, String>> {
    return ReclamationConfig.INCREMENT_MODE_VALUES.map { mode ->
        mode to when (mode) {
            0 -> stringResource(R.string.panel_reclamation_increment_click)
            1 -> stringResource(R.string.panel_reclamation_increment_hold)
            else -> mode.toString()
        }
    }
}
