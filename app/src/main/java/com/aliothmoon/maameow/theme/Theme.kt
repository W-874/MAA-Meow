package com.aliothmoon.maameow.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.data.preferences.AppSettingsManager

private val PureDarkBackground = Color(0xFF000000)

private fun ColorScheme.withDistinctSurfaceContainers(isDark: Boolean): ColorScheme {
    val tint = if (isDark) onSurface else primary
    fun container(amount: Float) = lerp(background, tint, amount)
    return copy(
        surface = background,
        surfaceContainerLowest = container(0.02f),
        surfaceContainerLow = container(0.06f),
        surfaceContainer = container(0.09f),
        surfaceContainerHigh = container(0.13f),
        surfaceContainerHighest = container(0.17f),
        surfaceVariant = container(0.17f),
    )
}

val MaaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(MaaDesignTokens.CornerRadius.inner),
    medium = RoundedCornerShape(MaaDesignTokens.CornerRadius.button),
    large = RoundedCornerShape(MaaDesignTokens.CornerRadius.card),
    extraLarge = RoundedCornerShape(MaaDesignTokens.CornerRadius.pill),
)

object MaaThemeAlphas {
    const val DISABLED = 0.38f
    const val SECONDARY = 0.60f
    const val MEDIUM = 0.74f
}

@Composable
fun OpaqueTheme(content: @Composable () -> Unit) {
    content()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaaMeowTheme(
    themeMode: AppSettingsManager.ThemeMode = AppSettingsManager.ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        AppSettingsManager.ThemeMode.SYSTEM -> systemDarkTheme
        AppSettingsManager.ThemeMode.WHITE -> false
        AppSettingsManager.ThemeMode.DARK, AppSettingsManager.ThemeMode.PURE_DARK -> true
    }
    val isPureDark = themeMode == AppSettingsManager.ThemeMode.PURE_DARK
    val colorScheme: ColorScheme = remember(themeMode, isDarkTheme, context) {
        val monet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (isDarkTheme) darkColorScheme() else lightColorScheme()
        }
        val base = if (isPureDark) {
            monet.copy(background = PureDarkBackground, surface = PureDarkBackground)
        } else monet
        base.withDistinctSurfaceContainers(isDarkTheme)
    }

    CompositionLocalProvider(
        LocalMaaDesignLanguage provides MaaDesignLanguage.MATERIAL_EXPRESSIVE,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MaaShapes,
            motionScheme = MotionScheme.expressive(),
        ) {
            ProvideLogPalette(isDark = isDarkTheme, content = content)
        }
    }
}
