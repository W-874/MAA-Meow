package com.aliothmoon.maameow.presentation.view.panel.fight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.resource.StageGroup
import com.aliothmoon.maameow.presentation.view.panel.TaskSettingsSectionTitle

@Composable
fun TodayStagesHint(
    stageGroups: List<StageGroup>,
    isResourceCollectionOpen: Boolean,
    stageTips: List<String>,
    todayName: String = "",
) {
    val activities = stageGroups.filter { !it.isPermanent }
    val todayOpenStages = stageGroups
        .find { it.isPermanent }
        ?.stages
        ?.filter { it.isOpenToday }
        .orEmpty()
    if (activities.isEmpty() && todayOpenStages.isEmpty()) return

    val activityCodes = remember(activities) {
        activities.flatMap { group -> group.stages.map { it.code } }.toSet()
    }
    val (activityTips, regularTips) = remember(stageTips, activityCodes) {
        stageTips.partition { tip ->
            tip.startsWith("｢") ||
                activityCodes.any { code -> code.isNotEmpty() && tip.startsWith("$code:") }
        }
    }
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TaskSettingsSectionTitle(
            title = stringResource(R.string.panel_fight_today_stage_hint_title, todayName),
        )
        Surface(
            onClick = { if (regularTips.isNotEmpty()) expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            enabled = regularTips.isNotEmpty(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        activityTips.forEach { tip ->
                            Text(
                                text = "· $tip",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tip.startsWith("｢")) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                },
                            )
                        }
                        if (isResourceCollectionOpen &&
                            activityTips.none { it.contains("资源收集") }
                        ) {
                            Text(
                                text = stringResource(R.string.panel_fight_resource_collection_open_tip),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (activityTips.isEmpty() && !isResourceCollectionOpen) {
                            Text(
                                text = stringResource(
                                    R.string.panel_fight_today_stage_summary,
                                    regularTips.size,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    if (regularTips.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = stringResource(
                                if (expanded) R.string.common_collapse else R.string.common_expand,
                            ),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(if (expanded) 180f else 0f),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        regularTips.forEach { tip ->
                            Text(
                                text = "· $tip",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Normal,
                                color = if (tip.trimStart().startsWith("(")) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
