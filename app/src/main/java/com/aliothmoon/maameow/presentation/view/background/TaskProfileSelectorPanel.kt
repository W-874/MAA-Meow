package com.aliothmoon.maameow.presentation.view.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import com.aliothmoon.maameow.presentation.benchmarkTestTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.TaskProfile
import com.aliothmoon.maameow.presentation.components.SettingActionButton
import com.aliothmoon.maameow.presentation.navigation.LocalMainBottomBarPadding
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun TaskProfileSelectorPanel(
    profiles: List<TaskProfile>,
    activeProfileId: String,
    onSwitchProfile: (String) -> Unit,
    onEditProfile: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onReorderProfile: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mainBottomBarPadding = LocalMainBottomBarPadding.current
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            if (from.index in profiles.indices && to.index in profiles.indices) {
                onReorderProfile(from.index, to.index)
            }
        },
    )

    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
            Text(
                text = stringResource(R.string.panel_profile_selector_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.panel_profile_selector_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(modifier = Modifier.size(10.dp))
        val itemCount = profiles.size + 1
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(bottom = mainBottomBarPadding + 8.dp),
        ) {
            itemsIndexed(
                items = profiles,
                key = { _, profile -> profile.id },
            ) { index, profile ->
                ReorderableItem(reorderableState, key = profile.id) {
                    val isActive = profile.id == activeProfileId
                    val enabledCount = profile.chain.count { it.enabled }
                    Surface(
                        onClick = { onSwitchProfile(profile.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .longPressDraggableHandle(),
                        shape = segmentedShape(index, itemCount),
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceBright
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isActive) {
                                                Icons.Default.Check
                                            } else {
                                                Icons.Default.Tune
                                            },
                                            contentDescription = null,
                                            tint = if (isActive) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isActive) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = if (isActive) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Medium
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.panel_profile_task_count,
                                            profile.chain.size,
                                            enabledCount,
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isActive) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.76f,
                                            )
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onEditProfile(profile.id) },
                                modifier = Modifier.benchmarkTestTag("edit_task_profile"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = stringResource(
                                        R.string.panel_profile_edit_tasks,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "create-profile") {
                SettingActionButton(
                    title = stringResource(R.string.panel_new_profile),
                    icon = Icons.Default.Add,
                    onClick = onCreateProfile,
                    enabled = profiles.size < 10,
                    shape = segmentedShape(itemCount - 1, itemCount),
                )
            }
        }
    }
}

private fun segmentedShape(index: Int, itemCount: Int): Shape {
    return when {
        itemCount <= 1 -> RoundedCornerShape(16.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 5.dp,
            bottomEnd = 5.dp,
        )
        index == itemCount - 1 -> RoundedCornerShape(
            topStart = 5.dp,
            topEnd = 5.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp,
        )
        else -> RoundedCornerShape(5.dp)
    }
}
