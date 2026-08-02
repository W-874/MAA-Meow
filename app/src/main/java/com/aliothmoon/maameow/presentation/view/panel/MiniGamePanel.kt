package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.resource.MiniGameTextRegistry
import com.aliothmoon.maameow.presentation.components.InfoCard
import com.aliothmoon.maameow.presentation.navigation.LocalMainBottomBarPadding
import com.aliothmoon.maameow.presentation.viewmodel.MiniGameDelegate
import com.aliothmoon.maameow.theme.MaaDesignTokens
import com.aliothmoon.maameow.utils.i18n.asString

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MiniGamePanel(
    modifier: Modifier = Modifier,
    delegate: MiniGameDelegate,
) {
    val state by delegate.state.collectAsStateWithLifecycle()
    val miniGames by delegate.miniGames.collectAsStateWithLifecycle()
    val currentGame = delegate.findGame(state.selectedTaskName)
    val tip = currentGame?.tip.asString().ifBlank { MiniGameTextRegistry.EMPTY_TIP.asString() }
    val isUnsupported = currentGame?.isUnsupported == true
    val currentGameDisplay = currentGame?.display.asString()
    val mainBottomBarPadding = LocalMainBottomBarPadding.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaaDesignTokens.Spacing.listHorizontal),
        contentPadding = PaddingValues(
            top = MaaDesignTokens.Spacing.xs,
            bottom = mainBottomBarPadding + MaaDesignTokens.Spacing.xs,
        ),
        verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.sm),
    ) {
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val useTwoColumns = maxWidth >= 288.dp
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    maxItemsInEachRow = if (useTwoColumns) 2 else 1,
                ) {
                    miniGames.forEachIndexed { index, game ->
                        val selected = state.selectedTaskName == game.value
                        val containerColor = when {
                            game.isUnsupported -> MaterialTheme.colorScheme.errorContainer
                            selected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceBright
                        }
                        val contentColor = when {
                            game.isUnsupported -> MaterialTheme.colorScheme.onErrorContainer
                            selected -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Surface(
                            onClick = { delegate.onTaskSelected(game.value) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = MaaDesignTokens.ListItem.minimumTouchTarget),
                            shape = miniGameOptionShape(
                                index = index,
                                itemCount = miniGames.size,
                                columns = if (useTwoColumns) 2 else 1,
                            ),
                            color = containerColor,
                            contentColor = contentColor,
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = MaaDesignTokens.Spacing.md,
                                    vertical = MaaDesignTokens.Spacing.xs,
                                ),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = game.display.asString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                RadioButton(
                                    selected = selected,
                                    onClick = { delegate.onTaskSelected(game.value) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (tip.isNotBlank()) {
            item {
                InfoCard(
                    title = currentGameDisplay.orEmpty(),
                    containerColor = if (isUnsupported) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (isUnsupported) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ) {
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUnsupported) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        if (delegate.isSecretFront(state.selectedTaskName)) {
            item {
                InfoCard(title = stringResource(R.string.panel_mini_game_ending)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                    ) {
                        MiniGameDelegate.ENDINGS.forEach { ending ->
                            FilterChip(
                                selected = state.selectedEnding == ending,
                                onClick = { delegate.onEndingSelected(ending) },
                                label = { Text(ending) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }
            }

            item {
                InfoCard(title = stringResource(R.string.panel_mini_game_preferred_events)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                    ) {
                        MiniGameDelegate.EVENTS.forEach { (value, display) ->
                            val selected = state.selectedEvent == value
                            FilterChip(
                                selected = selected,
                                onClick = { delegate.onEventSelected(value) },
                                label = { Text(display.asString()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun miniGameOptionShape(
    index: Int,
    itemCount: Int,
    columns: Int,
): RoundedCornerShape {
    val firstRow = index < columns
    val lastRowStart = ((itemCount - 1) / columns) * columns
    val lastRow = index >= lastRowStart
    val column = index % columns
    val isOnlyItemInLastRow = lastRow && itemCount % columns == 1
    val outerRadius = 16.dp
    val innerRadius = 5.dp

    return RoundedCornerShape(
        topStart = if (firstRow && column == 0) outerRadius else innerRadius,
        topEnd = if (firstRow && (column == columns - 1 || itemCount == 1)) {
            outerRadius
        } else {
            innerRadius
        },
        bottomStart = if (lastRow && column == 0) outerRadius else innerRadius,
        bottomEnd = if (lastRow && (column == columns - 1 || isOnlyItemInLastRow)) {
            outerRadius
        } else {
            innerRadius
        },
    )
}
