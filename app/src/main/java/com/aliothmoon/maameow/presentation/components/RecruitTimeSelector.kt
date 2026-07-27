package com.aliothmoon.maameow.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecruitTimeSelector(
    totalMinutes: Int,
    onTimeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val normalizedMinutes = totalMinutes.coerceIn(MIN_RECRUIT_MINUTES, MAX_RECRUIT_MINUTES)
    val hour = normalizedMinutes / 60
    val minute = normalizedMinutes % 60
    var showPicker by remember { mutableStateOf(false) }

    SettingRow(
        title = stringResource(R.string.panel_recruit_duration_title),
        description = stringResource(R.string.panel_recruit_duration_value, hour, minute),
        modifier = modifier,
        icon = null,
        enabled = enabled,
        onClick = { if (enabled) showPicker = true },
        trailing = {
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
            )
        },
    )

    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.panel_recruit_duration_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.panel_recruit_duration_picker_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SegmentedSettingsGroup {
                    item {
                        NumberStepperSettingRow(
                            title = stringResource(R.string.panel_recruit_duration_hours),
                            value = hour,
                            range = 1..9,
                            icon = null,
                            onValueChange = { nextHour ->
                                onTimeChange(nextHour * 60 + if (nextHour == 9) 0 else minute)
                            },
                        )
                    }
                    item {
                        NumberStepperSettingRow(
                            title = stringResource(R.string.panel_recruit_duration_minutes),
                            value = minute,
                            range = 0..55,
                            step = 5,
                            enabled = hour < 9,
                            icon = null,
                            onValueChange = { nextMinute ->
                                onTimeChange(hour * 60 + nextMinute)
                            },
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { showPicker = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.common_done))
                }
            }
        }
    }
}

private const val MIN_RECRUIT_MINUTES = 60
private const val MAX_RECRUIT_MINUTES = 540
