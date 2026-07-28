package com.aliothmoon.maameow.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.theme.MaaDesignTokens

internal val LocalSettingItemShape = compositionLocalOf<Shape> { RoundedCornerShape(16.dp) }

@Composable
fun SettingActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = shape ?: LocalSettingItemShape.current,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.38f),
            disabledContentColor = contentColor.copy(alpha = 0.72f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    leadingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: ImageVector? = Icons.Rounded.Settings,
    enabled: Boolean = true,
    titleContent: (@Composable () -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    headlineOverlay: @Composable BoxScope.() -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    val shape = LocalSettingItemShape.current
    val disabledAlpha = if (enabled) 1f else 0.38f
    val density = LocalDensity.current
    val dynamicPadding = (4 * density.fontScale).dp
    val isSingleLine = description == null
    val configuration = LocalConfiguration.current
    val baseSingleLineHeight = when {
        configuration.screenHeightDp < MaaDesignTokens.ListItem.denseHeightThresholdDp ->
            MaaDesignTokens.ListItem.singleLineDense
        configuration.screenHeightDp < MaaDesignTokens.ListItem.compactHeightThresholdDp ->
            MaaDesignTokens.ListItem.singleLineCompact
        else -> MaaDesignTokens.ListItem.singleLine
    }
    val singleLineHeight = maxOf(
        baseSingleLineHeight,
        MaaDesignTokens.ListItem.minimumTouchTarget * density.fontScale,
    )
    val rowModifier = modifier
        .fillMaxWidth()
        .then(if (isSingleLine) Modifier.height(singleLineHeight) else Modifier)
    val colors = ListItemDefaults.colors(
        containerColor = containerColor,
        contentColor = titleColor,
        leadingContentColor = leadingColor,
        trailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        supportingContentColor = descriptionColor,
        disabledContainerColor = containerColor,
        disabledContentColor = titleColor,
        disabledLeadingContentColor = leadingColor,
        disabledTrailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledSupportingContentColor = descriptionColor,
    )
    val shapes = ListItemDefaults.shapes(
        shape = shape,
        pressedShape = shape,
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
                .then(
                    if (isSingleLine) Modifier else Modifier.padding(top = dynamicPadding)
                ),
        ) {
            titleContent?.invoke() ?: Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
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
            modifier = rowModifier,
            onClick = onClick,
            enabled = enabled,
            colors = colors,
            shapes = shapes,
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = leadingContent,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
            content = headlineContent,
        )
    } else {
        ListItem(
            modifier = rowModifier.clip(shape),
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
    singleLine: Boolean = false,
    singleLineActionStyle: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val menu: @Composable () -> Unit = {
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuGroup(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                shapes = MenuDefaults.groupShapes(),
            ) {
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
    SettingRow(
        title = title,
        description = if (singleLine) null else optionLabel(selected),
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        onClick = { expanded = !expanded },
        trailing = if (singleLine) {
            {
                Box(
                    modifier = if (singleLineActionStyle) {
                        Modifier.widthIn(min = 64.dp)
                    } else {
                        Modifier
                    },
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        text = optionLabel(selected),
                        style = if (singleLineActionStyle) {
                            MaterialTheme.typography.labelLarge
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = if (singleLineActionStyle) {
                            Modifier.padding(horizontal = 8.dp)
                        } else {
                            Modifier
                        },
                    )
                    menu()
                }
            }
        } else {
            {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                )
            }
        },
        headlineOverlay = {
            if (!singleLine) {
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    menu()
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
    modifier: Modifier = Modifier,
) {
    Switch(
        modifier = modifier,
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

@Composable
fun NumberStepperSettingRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = Icons.Rounded.Settings,
    enabled: Boolean = true,
    step: Int = 1,
) {
    SettingRow(
        title = title,
        description = description,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    enabled = enabled && value > range.first,
                    onClick = { onValueChange((value - step).coerceIn(range)) },
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = null)
                }
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.widthIn(min = 28.dp),
                )
                IconButton(
                    enabled = enabled && value < range.last,
                    onClick = { onValueChange((value + step).coerceIn(range)) },
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
            }
        },
    )
}
