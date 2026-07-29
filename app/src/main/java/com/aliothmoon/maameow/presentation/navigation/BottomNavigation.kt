package com.aliothmoon.maameow.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.aliothmoon.maameow.presentation.benchmarkTestTag
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Routes

sealed class BottomNavTab(
    val route: String, @param:StringRes val labelRes: Int, val icon: ImageVector
) {
    data object HOME : BottomNavTab(
        route = Routes.HOME, labelRes = R.string.bottom_nav_home, icon = Icons.Default.Home
    )

    data object BACKGROUND : BottomNavTab(
        route = Routes.BACKGROUND_TASK,
        labelRes = R.string.bottom_nav_background_task,
        icon = Icons.Default.PlayArrow
    )

    data object SCHEDULE : BottomNavTab(
        route = Routes.SCHEDULE,
        labelRes = R.string.bottom_nav_schedule,
        icon = Icons.Default.DateRange
    )

    data object SETTINGS : BottomNavTab(
        route = Routes.SETTINGS,
        labelRes = R.string.bottom_nav_settings,
        icon = Icons.Default.Settings
    )

    companion object {
        val all = listOf(HOME, BACKGROUND, SCHEDULE, SETTINGS)
    }
}

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        windowInsets = WindowInsets.navigationBars,
    ) {
        BottomNavTab.all.forEach { tab ->
            val label = stringResource(tab.labelRes)
            val selected = currentRoute == tab.route
            NavigationBarItem(
                modifier = Modifier.benchmarkTestTag("nav_${tab.route}"),
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = label,
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(),
            )
        }
    }
}
