package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews

private const val BADGE_MAX = 99

/** One destination of [AppNavigationBar]; a positive [badgeCount] shows a count badge ("99+" above 99). */
data class NavBarItem(
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int? = null,
)

const val APP_NAVIGATION_BAR_TAG = "app_navigation_bar"

/** Bottom navigation bar for top-level destinations. Stateless: the caller owns [selectedIndex]. */
@Composable
fun AppNavigationBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier.testTag(APP_NAVIGATION_BAR_TAG)) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                icon = { NavBarIcon(item) },
                label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

/** The item's icon with its count badge ("99+" above [BADGE_MAX]); shared by the bar and the rail. */
@Composable
internal fun NavBarIcon(item: NavBarItem) {
    val count = item.badgeCount
    if (count != null && count > 0) {
        BadgedBox(badge = { Badge { Text(if (count > BADGE_MAX) "$BADGE_MAX+" else "$count") } }) {
            Icon(item.icon, contentDescription = null)
        }
    } else {
        Icon(item.icon, contentDescription = null)
    }
}

@ThemePreviews
@Composable
private fun AppNavigationBarPreview() {
    PreviewTheme {
        AppNavigationBar(
            items =
                listOf(
                    NavBarItem("Home", Icons.Filled.Home),
                    NavBarItem("Profile", Icons.Filled.Person, badgeCount = 3),
                    NavBarItem("Settings", Icons.Filled.Settings, badgeCount = 120),
                ),
            selectedIndex = 0,
            onSelect = {},
        )
    }
}
