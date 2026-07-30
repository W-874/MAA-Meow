package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.presentation.components.ExpressiveSwitch
import com.aliothmoon.maameow.presentation.components.SettingActionButton
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 左侧任务列表（支持模式切换、拖拽排序、勾选、新增任务入口）
 */
@Composable
fun TaskListPanel(
    nodes: List<TaskChainNode>,
    selectedNodeId: String?,
    isEditMode: Boolean,
    isAddingTask: Boolean,
    isProfileMode: Boolean,
    onNodeEnabledChange: (String, Boolean) -> Unit,
    onNodeSelected: (String) -> Unit,
    onNodeMove: (Int, Int) -> Unit,
    onToggleEditMode: () -> Unit,
    onToggleAddingTask: () -> Unit,
    onToggleProfileMode: () -> Unit,
    showManagementActions: Boolean = true,
    showAddTaskItem: Boolean = false,
    useIntrinsicWidth: Boolean = true,
    expressiveStyle: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val widthModifier = if (useIntrinsicWidth) {
        Modifier.width(IntrinsicSize.Max)
    } else {
        Modifier.fillMaxWidth()
    }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            if (from.index in nodes.indices && to.index in nodes.indices) {
                onNodeMove(from.index, to.index)
            }
        },
    )

    Column(modifier = modifier.then(widthModifier)) {
        if (showManagementActions) {
        // 配置选择按钮 - 在编辑任务按钮上方
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleProfileMode() },
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isProfileMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(
                1.dp,
                if (isProfileMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isProfileMode) 2.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isProfileMode) Icons.Default.Check else Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isProfileMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isProfileMode) stringResource(R.string.common_done) else stringResource(
                        R.string.panel_task_list_edit_config
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isProfileMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (isProfileMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 编辑任务按钮 - 具备高亮状态
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleEditMode() },
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isEditMode) 2.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEditMode) stringResource(R.string.common_done) else stringResource(
                        R.string.panel_task_list_edit_tasks
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isEditMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 新增任务按钮 - 仅在编辑模式下显示
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAddingTask() },
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAddingTask) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    border = if (isAddingTask) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    } else {
                        null
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isAddingTask) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.panel_task_list_add),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isAddingTask) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (isAddingTask) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .then(widthModifier)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (expressiveStyle) 2.dp else 6.dp)
        ) {
            itemsIndexed(
                items = nodes,
                key = { _, node -> node.id },
            ) { index, node ->
                ReorderableItem(reorderableState, key = node.id) {
                    TaskNodeRow(
                        node = node,
                        isSelected = selectedNodeId == node.id,
                        isEditMode = isEditMode,
                        expressiveStyle = expressiveStyle,
                        itemIndex = index,
                        itemCount = nodes.size + if (showAddTaskItem) 1 else 0,
                        onEnabledChange = { enabled -> onNodeEnabledChange(node.id, enabled) },
                        onSelected = { onNodeSelected(node.id) },
                        modifier = Modifier.longPressDraggableHandle(),
                    )
                }
            }
            if (showAddTaskItem) {
                item(key = "add-task") {
                    SettingActionButton(
                        title = stringResource(R.string.panel_task_list_add),
                        icon = Icons.Default.Add,
                        onClick = onToggleAddingTask,
                        shape = expressiveSegmentedShape(nodes.size, nodes.size + 1),
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskNodeRow(
    node: TaskChainNode,
    isSelected: Boolean,
    isEditMode: Boolean,
    expressiveStyle: Boolean,
    itemIndex: Int,
    itemCount: Int,
    onEnabledChange: (Boolean) -> Unit,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showSelection = isSelected && !expressiveStyle
    val itemShape = if (expressiveStyle) {
        expressiveSegmentedShape(itemIndex, itemCount)
    } else {
        RoundedCornerShape(4.dp)
    }
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = itemShape,
        colors = CardDefaults.cardColors(
            containerColor = if (showSelection) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (expressiveStyle) {
                MaterialTheme.colorScheme.surfaceBright
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = if (showSelection) BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        ) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelected() }
                .padding(
                    horizontal = if (expressiveStyle) 14.dp else 4.dp,
                    vertical = if (expressiveStyle) 11.dp else 6.dp,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (expressiveStyle) {
                Icon(
                    imageVector = taskConfigIcon(node.config),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (showSelection) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (showSelection) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = taskConfigSummary(node.config),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (showSelection) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ExpressiveSwitch(
                    checked = node.enabled,
                    onCheckedChange = onEnabledChange,
                )
            } else {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Checkbox(
                        checked = node.enabled,
                        onCheckedChange = onEnabledChange,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (showSelection) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (showSelection) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (isEditMode) Icons.Default.DragHandle else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private fun expressiveSegmentedShape(index: Int, itemCount: Int) = when {
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
