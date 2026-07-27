package com.aliothmoon.maameow.presentation.view.panel.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aliothmoon.maameow.presentation.components.SettingDropdown

/**
 * 材料选择按钮组
 */
@Composable
fun ItemButtonGroup(
    modifier: Modifier = Modifier,
    label: String,
    selectedValue: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    displayMapper: (String) -> String = { it },
) {
    SettingDropdown(
        title = label,
        selected = selectedValue,
        options = items,
        optionLabel = { displayMapper(it) },
        onSelected = onItemSelected,
        modifier = modifier,
        icon = null,
    )
}
