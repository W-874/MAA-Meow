package com.aliothmoon.maameow.presentation.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T, K : Any> ReorderableFlowRow(
    items: List<T>,
    itemKey: (T) -> K,
    onOrderChanged: (List<T>) -> Unit,
    onDraggingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(6.dp),
    content: @Composable (item: T, isDragging: Boolean, dragModifier: Modifier) -> Unit,
) {
    var visualItems by remember { mutableStateOf(items) }
    var draggedKey by remember { mutableStateOf<K?>(null) }
    var pointerInRoot by remember { mutableStateOf(Offset.Zero) }
    var grabOffset by remember { mutableStateOf(Offset.Zero) }
    val itemBounds = remember { mutableStateMapOf<K, Rect>() }

    LaunchedEffect(items, draggedKey) {
        if (draggedKey == null && visualItems != items) visualItems = items
    }

    LookaheadScope {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = verticalArrangement,
        ) {
            visualItems.forEach { item ->
                val key = itemKey(item)
                key(key) {
                    val isDragging = draggedKey == key
                    val dragModifier = Modifier
                        .animateBounds(
                            lookaheadScope = this@LookaheadScope,
                            boundsTransform = { _, _ ->
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                )
                            },
                        )
                        .onGloballyPositioned { itemBounds[key] = it.boundsInRoot() }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            val bounds = itemBounds[key]
                            if (isDragging && bounds != null) {
                                translationX = pointerInRoot.x - grabOffset.x - bounds.center.x
                                translationY = pointerInRoot.y - grabOffset.y - bounds.center.y
                                scaleX = 1.06f
                                scaleY = 1.06f
                                shadowElevation = 8.dp.toPx()
                            }
                        }
                        .pointerInput(key) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startPosition ->
                                    val bounds = itemBounds[key] ?: return@detectDragGesturesAfterLongPress
                                    pointerInRoot = bounds.topLeft + startPosition
                                    grabOffset = pointerInRoot - bounds.center
                                    draggedKey = key
                                    onDraggingChanged(true)
                                },
                                onDragCancel = {
                                    visualItems = items
                                    draggedKey = null
                                    onDraggingChanged(false)
                                },
                                onDragEnd = {
                                    onOrderChanged(visualItems)
                                    draggedKey = null
                                    onDraggingChanged(false)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    pointerInRoot += dragAmount
                                    val targetKey = visualItems
                                        .asSequence()
                                        .map(itemKey)
                                        .firstOrNull { candidate ->
                                            candidate != key && itemBounds[candidate]
                                                ?.contains(pointerInRoot) == true
                                        } ?: return@detectDragGesturesAfterLongPress
                                    val from = visualItems.indexOfFirst { itemKey(it) == key }
                                    val to = visualItems.indexOfFirst { itemKey(it) == targetKey }
                                    if (from >= 0 && to >= 0 && from != to) {
                                        visualItems = visualItems.toMutableList().apply {
                                            add(to, removeAt(from))
                                        }
                                    }
                                },
                            )
                        }

                    content(item, isDragging, dragModifier)
                }
            }
        }
    }
}
