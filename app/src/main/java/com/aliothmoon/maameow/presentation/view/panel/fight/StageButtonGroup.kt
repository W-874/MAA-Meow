package com.aliothmoon.maameow.presentation.view.panel.fight

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aliothmoon.maameow.presentation.components.SettingDropdown

/**
 * 关卡选择按钮组
 */
@Composable
fun StageButtonGroup(
    modifier: Modifier = Modifier,
    label: String,
    selectedValue: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    displayMapper: (String) -> String = { it },
    isOpenCheck: (String) -> Boolean = { true },
) {
    val availableItems = items.filter(isOpenCheck).toMutableList().apply {
        if (selectedValue !in this && selectedValue in items) add(0, selectedValue)
    }
    SettingDropdown(
        title = label,
        selected = selectedValue,
        options = availableItems,
        optionLabel = { displayMapper(it) },
        onSelected = onItemSelected,
        modifier = modifier,
        icon = null,
    )
}
