package com.aliothmoon.maameow.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

internal val LocalSettingItemShape = compositionLocalOf<Shape> { RoundedCornerShape(16.dp) }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: ImageVector? = Icons.Rounded.Settings,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    headlineOverlay: @Composable BoxScope.() -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    val shape = LocalSettingItemShape.current
    val disabledAlpha = if (enabled) 1f else 0.38f
    val dynamicPadding = (4 * LocalDensity.current.fontScale).dp
    val colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceBright,
        contentColor = titleColor,
        leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        trailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        supportingContentColor = descriptionColor,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
        disabledContentColor = titleColor,
        disabledLeadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledSupportingContentColor = descriptionColor,
    )
    val shapes = ListItemDefaults.shapes(
        shape = shape,
        pressedShape = RoundedCornerShape(16.dp),
        selectedShape = shape,
        focusedShape = shape,
        hoveredShape = shape,
    )
    val leadingContent: (@Composable () -> Unit)? = icon?.let {
        {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(24.dp).alpha(disabledAlpha),
            )
        }
    }
    val supportingContent: (@Composable () -> Unit)? = description?.let {
        {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(disabledAlpha).padding(bottom = dynamicPadding),
            )
        }
    }
    val headlineContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .alpha(disabledAlpha)
                .padding(
                    top = dynamicPadding,
                    bottom = if (description == null) dynamicPadding else 0.dp,
                ),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            headlineOverlay()
        }
    }
    val trailingContent: @Composable () -> Unit = {
        Box(modifier = Modifier.alpha(disabledAlpha), contentAlignment = Alignment.Center) {
            trailing?.invoke() ?: Spacer(Modifier.size(0.dp))
        }
    }

    if (onClick != null) {
        ListItem(
            selected = false,
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
            enabled = enabled,
            colors = colors,
            shapes = shapes,
            leadingContent = leadingContent,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
            content = headlineContent,
        )
    } else {
        ListItem(
            modifier = modifier.fillMaxWidth().clip(shape),
            colors = colors,
            leadingContent = leadingContent,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
            content = headlineContent,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> SettingDropdown(
    title: String,
    selected: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Rounded.Settings,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    SettingRow(
        title = title,
        description = optionLabel(selected),
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        onClick = { expanded = !expanded },
        headlineOverlay = {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                DropdownMenuPopup(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                        options.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                selected = option == selected,
                                onClick = {
                                    onSelected(option)
                                    expanded = false
                                },
                                text = { Text(optionLabel(option)) },
                                shapes = MenuDefaults.itemShape(index, options.size),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    Switch(
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedIconColor = MaterialTheme.colorScheme.primary,
            uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}
