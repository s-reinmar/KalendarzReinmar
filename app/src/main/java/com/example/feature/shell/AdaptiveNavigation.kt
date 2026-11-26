package com.example.feature.shell

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AppIcons

private data class NavDestinationMeta(
    val tab: AppTab,
    val labelRes: Int,
    val iconRes: Int,
    val contentDescriptionRes: Int,
    val testTag: String,
)

private val NAV_DESTINATIONS = listOf(
    NavDestinationMeta(
        tab = AppTab.CALENDAR,
        labelRes = R.string.nav_calendar,
        iconRes = AppIcons.calendar,
        contentDescriptionRes = R.string.cd_calendar_icon,
        testTag = "nav_tab_calendar",
    ),
    NavDestinationMeta(
        tab = AppTab.MAP,
        labelRes = R.string.nav_map,
        iconRes = AppIcons.map,
        contentDescriptionRes = R.string.cd_map_icon,
        testTag = "nav_tab_map",
    ),
    NavDestinationMeta(
        tab = AppTab.NOTES,
        labelRes = R.string.nav_notes,
        iconRes = AppIcons.notes,
        contentDescriptionRes = R.string.cd_notes_icon,
        testTag = "nav_tab_notes",
    ),
    NavDestinationMeta(
        tab = AppTab.SETTINGS,
        labelRes = R.string.nav_settings,
        iconRes = AppIcons.settings,
        contentDescriptionRes = R.string.cd_settings_icon,
        testTag = "nav_tab_settings",
    ),
)

@Composable
internal fun AppNavigationRail(
    activeTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
) {
    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        NAV_DESTINATIONS.forEach { dest ->
            NavigationRailItem(
                selected = activeTab == dest.tab,
                onClick = { onSelectTab(dest.tab) },
                icon = {
                    Icon(
                        painter = painterResource(dest.iconRes),
                        contentDescription = stringResource(dest.contentDescriptionRes),
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = { Text(stringResource(dest.labelRes)) },
                colors = itemColors,
                modifier = Modifier.testTag(dest.testTag),
            )
        }
    }
}

@Composable
internal fun AppNavigationBar(
    activeTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = windowInsets,
    ) {
        NAV_DESTINATIONS.forEach { dest ->
            NavigationBarItem(
                selected = activeTab == dest.tab,
                onClick = { onSelectTab(dest.tab) },
                icon = {
                    Icon(
                        painter = painterResource(dest.iconRes),
                        contentDescription = stringResource(dest.contentDescriptionRes),
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = { Text(stringResource(dest.labelRes)) },
                colors = itemColors,
                modifier = Modifier.testTag(dest.testTag),
            )
        }
    }
}
