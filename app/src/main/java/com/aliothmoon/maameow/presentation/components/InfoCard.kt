package com.aliothmoon.maameow.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.theme.MaaDesignTokens

@Composable
private fun BaseCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = MaaDesignTokens.Card.elevation),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    title: String = "",
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    BaseCard(
        modifier = modifier,
        contentPadding = PaddingValues(MaaDesignTokens.Card.innerPadding),
        containerColor = containerColor,
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.padding(bottom = MaaDesignTokens.Spacing.sm),
            )
        }
        content()
    }
}

class SegmentedSettingsScope internal constructor() {
    internal data class Item(
        val shape: Shape?,
        val content: @Composable () -> Unit,
    )

    internal val items = mutableListOf<Item>()

    fun item(
        shape: Shape? = null,
        content: @Composable () -> Unit,
    ) {
        items += Item(shape, content)
    }
}

private fun segmentedItemShape(index: Int, lastIndex: Int) = when {
    lastIndex == 0 -> RoundedCornerShape(16.dp)
    index == 0 -> RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 5.dp,
        bottomEnd = 5.dp,
    )
    index == lastIndex -> RoundedCornerShape(
        topStart = 5.dp,
        topEnd = 5.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp,
    )
    else -> RoundedCornerShape(5.dp)
}

@Composable
fun animatedSegmentedItemShape(
    index: Int,
    itemCount: Int,
    expanded: Boolean,
): RoundedCornerShape {
    val lastIndex = itemCount - 1
    val collapsedTopRadius = if (itemCount <= 1 || index == 0) 16.dp else 5.dp
    val collapsedBottomRadius = if (itemCount <= 1 || index == lastIndex) 16.dp else 5.dp
    val animationSpec = tween<androidx.compose.ui.unit.Dp>(durationMillis = 220)
    val topRadius by animateDpAsState(
        targetValue = if (expanded) 16.dp else collapsedTopRadius,
        animationSpec = animationSpec,
        label = "segmentedItemTopRadius",
    )
    val bottomRadius by animateDpAsState(
        targetValue = if (expanded) 16.dp else collapsedBottomRadius,
        animationSpec = animationSpec,
        label = "segmentedItemBottomRadius",
    )
    return RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius,
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SegmentedSettingsGroup(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    content: SegmentedSettingsScope.() -> Unit,
) {
    val items = SegmentedSettingsScope().apply(content).items
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        items.forEachIndexed { index, item ->
            val shape = item.shape ?: segmentedItemShape(index, items.lastIndex)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                color = containerColor,
            ) {
                CompositionLocalProvider(LocalSettingItemShape provides shape) {
                    item.content()
                }
            }
        }
    }
}
