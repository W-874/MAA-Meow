package com.aliothmoon.maameow.presentation.view.panel.roguelike

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.domain.enums.UiUsageConstants.Roguelike as RoguelikeUi
import com.aliothmoon.maameow.presentation.components.ExpressiveSwitch
import com.aliothmoon.maameow.presentation.components.SettingRow
import kotlin.math.roundToInt

@Composable
fun RoguelikeDifficultyButtonGroup(
    label: String,
    selectedValue: Int,
    theme: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxDifficulty = RoguelikeUi.getMaxDifficultyForTheme(theme)
    val enabled = selectedValue != -1
    val sliderPosition = when (selectedValue) {
        Int.MAX_VALUE -> maxDifficulty + 1
        -1 -> 0
        else -> selectedValue.coerceIn(0, maxDifficulty)
    }
    val valueLabel = when {
        !enabled -> stringResource(R.string.common_not_switch)
        sliderPosition == 0 -> stringResource(R.string.panel_roguelike_difficulty_min)
        sliderPosition == maxDifficulty + 1 -> stringResource(
            R.string.panel_roguelike_difficulty_max,
            maxDifficulty,
        )
        else -> sliderPosition.toString()
    }

    Column(modifier = modifier) {
        SettingRow(
            title = label,
            description = valueLabel,
            icon = null,
            onClick = {
                onValueChange(if (enabled) -1 else Int.MAX_VALUE)
            },
            trailing = {
                ExpressiveSwitch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        onValueChange(if (checked) Int.MAX_VALUE else -1)
                    },
                )
            },
        )
        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Slider(
                    value = sliderPosition.toFloat(),
                    onValueChange = { position ->
                        val step = position.roundToInt().coerceIn(0, maxDifficulty + 1)
                        onValueChange(if (step == maxDifficulty + 1) Int.MAX_VALUE else step)
                    },
                    valueRange = 0f..(maxDifficulty + 1).toFloat(),
                    steps = maxDifficulty,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.panel_roguelike_difficulty_min),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = valueLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(
                            R.string.panel_roguelike_difficulty_max,
                            maxDifficulty,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
