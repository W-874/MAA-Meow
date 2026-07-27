package com.aliothmoon.maameow.presentation.view.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.TaskProfile

@Composable
fun TaskProfileSelectorPanel(
    profiles: List<TaskProfile>,
    activeProfileId: String,
    onSwitchProfile: (String) -> Unit,
    onEditProfile: (String) -> Unit,
    onCreateProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
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
        }

        item {
            val itemCount = profiles.size + 1
            Column(
                modifier = Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                profiles.forEachIndexed { index, profile ->
                    val isActive = profile.id == activeProfileId
                    val enabledCount = profile.chain.count { it.enabled }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = segmentedShape(index, itemCount),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceBright
                            },
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSwitchProfile(profile.id) }
                                    .padding(vertical = 4.dp),
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isActive) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Medium
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (isActive) {
                                            Text(
                                                text = stringResource(R.string.panel_profile_active),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(
                                            R.string.panel_profile_task_count,
                                            profile.chain.size,
                                            enabledCount,
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                            IconButton(onClick = { onEditProfile(profile.id) }) {
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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = profiles.size < 10,
                            onClick = onCreateProfile,
                        ),
                    shape = segmentedShape(itemCount - 1, itemCount),
                    colors = CardDefaults.cardColors(
                        containerColor = if (profiles.size < 10) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (profiles.size < 10) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.panel_new_profile),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (profiles.size < 10) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
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
