package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.presentation.viewmodel.ToolboxTab

@Composable
fun ToolboxNavigation(
    tabs: List<ToolboxTab>,
    currentTab: ToolboxTab,
    onSelect: (ToolboxTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = currentTab == tab
            val label = stringResource(tab.labelRes)
            Surface(
                modifier = (if (selected) {
                    Modifier.weight(1f).widthIn(min = 48.dp)
                } else {
                    Modifier.size(48.dp)
                })
                    .heightIn(min = 48.dp)
                    .animateContentSize()
                    .selectable(
                        selected = selected,
                        onClick = { onSelect(tab) },
                        role = Role.Tab,
                    ),
                shape = MaterialTheme.shapes.large,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clipToBounds(),
                    horizontalArrangement = if (selected) {
                        Arrangement.Start
                    } else {
                        Arrangement.Center
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                    )
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

private fun ToolboxTab.icon(): ImageVector = when (this) {
    ToolboxTab.MINI_GAME -> Icons.Default.Home
    ToolboxTab.RECRUIT_CALC -> Icons.Default.Search
    ToolboxTab.DEPOT -> Icons.Default.Inventory2
    ToolboxTab.OPER_BOX -> Icons.Default.Settings
    ToolboxTab.GACHA -> Icons.Default.Star
}
