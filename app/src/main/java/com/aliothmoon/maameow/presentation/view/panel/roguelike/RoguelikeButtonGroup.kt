package com.aliothmoon.maameow.presentation.view.panel.roguelike

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aliothmoon.maameow.presentation.components.SettingDropdown

/**
 * 通用按钮组组件
 */
@Composable
fun RoguelikeButtonGroup(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingDropdown(
        title = label,
        selected = selectedValue,
        options = options.map { it.first },
        optionLabel = { value -> options.firstOrNull { it.first == value }?.second ?: value },
        onSelected = onValueChange,
        modifier = modifier,
        icon = null,
    )
}
