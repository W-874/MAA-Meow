package com.aliothmoon.maameow.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MaaDesignLanguage {
    MATERIAL_EXPRESSIVE,
    MIUIX,
}

@Immutable
data class MaaComponentTokens(
    val compactControlHeight: Dp,
    val controlHeight: Dp,
    val prominentControlHeight: Dp,
    val navigationIconSize: Dp,
    val contentMaxWidth: Dp,
)

private val MaterialExpressiveComponentTokens = MaaComponentTokens(
    compactControlHeight = 40.dp,
    controlHeight = 48.dp,
    prominentControlHeight = 56.dp,
    navigationIconSize = 24.dp,
    contentMaxWidth = 840.dp,
)

val LocalMaaDesignLanguage = staticCompositionLocalOf {
    MaaDesignLanguage.MATERIAL_EXPRESSIVE
}

val LocalMaaComponentTokens = staticCompositionLocalOf {
    MaterialExpressiveComponentTokens
}

object MaaDesignSystem {
    val language: MaaDesignLanguage
        @Composable @ReadOnlyComposable get() = LocalMaaDesignLanguage.current

    val components: MaaComponentTokens
        @Composable @ReadOnlyComposable get() = LocalMaaComponentTokens.current
}
