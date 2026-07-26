package com.aliothmoon.maameow.presentation.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    internal val items = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        items += content
    }
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
        items.forEachIndexed { index, itemContent ->
            val shape = when {
                items.size == 1 -> RoundedCornerShape(16.dp)
                index == 0 -> RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 5.dp,
                    bottomEnd = 5.dp,
                )
                index == items.lastIndex -> RoundedCornerShape(
                    topStart = 5.dp,
                    topEnd = 5.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp,
                )
                else -> RoundedCornerShape(5.dp)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                color = containerColor,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    itemContent()
                }
            }
        }
    }
}
