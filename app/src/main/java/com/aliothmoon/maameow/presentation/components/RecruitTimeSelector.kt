package com.aliothmoon.maameow.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import java.util.Locale

@Composable
fun RecruitTimeSelector(
    totalMinutes: Int,
    onTimeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    editable: Boolean = enabled,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val normalizedMinutes = totalMinutes.coerceIn(MIN_RECRUIT_MINUTES, MAX_RECRUIT_MINUTES)
    val hour = normalizedMinutes / 60
    val minute = normalizedMinutes % 60
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingRow(
            title = stringResource(R.string.panel_recruit_duration_title),
            titleContent = { RecruitTimeTitle(totalMinutes = normalizedMinutes) },
            icon = null,
            enabled = enabled,
            containerColor = containerColor,
            titleColor = contentColor,
            descriptionColor = supportingColor,
            onClick = if (enabled && editable) {
                { expanded = !expanded }
            } else null,
            trailing = if (editable) {
                {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = stringResource(
                            if (expanded) {
                                R.string.common_collapse
                            } else {
                                R.string.common_expand
                            },
                        ),
                    )
                }
            } else null,
        )
        AnimatedVisibility(
            visible = expanded && enabled && editable,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            SegmentedSettingsGroup(
                modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 12.dp),
            ) {
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
        }
    }
}

@Composable
fun RecruitTimeTitle(totalMinutes: Int) {
    val normalizedMinutes = totalMinutes.coerceIn(MIN_RECRUIT_MINUTES, MAX_RECRUIT_MINUTES)
    val hour = normalizedMinutes / 60
    val minute = normalizedMinutes % 60

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.panel_recruit_duration_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(6.dp),
        ) {
            Text(
                text = String.format(Locale.ROOT, "%02d:%02d", hour, minute),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun RecruitTimeBadge(totalMinutes: Int) {
    val normalizedMinutes = totalMinutes.coerceIn(MIN_RECRUIT_MINUTES, MAX_RECRUIT_MINUTES)
    val hour = normalizedMinutes / 60
    val minute = normalizedMinutes % 60

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.panel_recruit_confirm_time_label),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = String.format(Locale.ROOT, "%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
fun RecruitConfirmationSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    totalMinutes: Int,
    showTime: Boolean,
    timeEditable: Boolean,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
) {
    SettingRow(
        title = title,
        icon = null,
        onClick = { onCheckedChange(!checked) },
        titleContent = {
            Row(
                modifier = Modifier.heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (checked && showTime) {
                    RecruitTimeBadge(totalMinutes = totalMinutes)
                    if (timeEditable) {
                        IconButton(
                            onClick = { onEditingChange(!editing) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(
                                    R.string.panel_recruit_edit_confirm_time,
                                ),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        },
        trailing = {
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

private const val MIN_RECRUIT_MINUTES = 60
private const val MAX_RECRUIT_MINUTES = 540
