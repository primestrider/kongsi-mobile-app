package com.primestrider.kongsi.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.icon.AppIcons
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews

private const val BADGE_MAX = 99

/** How far the raised action button rises above the bar's top edge. */
private val ACTION_RISE: Dp = 20.dp

/** Height of a Material 3 navigation bar (without the system navigation bar inset). */
private val BAR_HEIGHT: Dp = 80.dp

/** Diameter of the raised action button. */
private val ACTION_SIZE: Dp = 60.dp

/** One destination of [AppNavigationBar]; a positive [badgeCount] shows a count badge ("99+" above 99). */
data class NavBarItem(
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int? = null,
)

/**
 * The app's main action, shown as a raised round button in the middle of the bar (like a payment app's scan button)
 * and as a FAB at the top of the navigation rail. It is an action, not a destination: it is never selected.
 */
data class NavBarAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

const val APP_NAVIGATION_BAR_TAG = "app_navigation_bar"

/**
 * Bottom navigation bar for top-level destinations. Stateless: the caller owns [selectedIndex]. With a [centerAction]
 * the destinations split around a raised button in the middle (the first half of [items] on the start side).
 */
@Composable
fun AppNavigationBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    centerAction: NavBarAction? = null,
) {
    val split = items.size / 2
    // The raised button sits in a strip above the bar: drawn outside the bar it would be clipped and, past the
    // bar's bounds, would not receive touches.
    Box(modifier.testTag(APP_NAVIGATION_BAR_TAG)) {
        Column(Modifier.padding(top = if (centerAction != null) ACTION_RISE else 0.dp)) {
            // A hairline over the screen's background instead of a tonal slab.
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                items.forEachIndexed { index, item ->
                    if (centerAction != null && index == split) CenterActionSlot(centerAction)
                    NavigationBarItem(
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        icon = { NavBarIcon(item) },
                        label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = navItemColors(),
                    )
                }
                if (centerAction != null && split == items.size) CenterActionSlot(centerAction)
            }
        }
        if (centerAction != null) RaisedActionButton(centerAction, Modifier.align(Alignment.TopCenter))
    }
}

/** The slot in the bar under the raised button: holds the label and is the node TalkBack reads and taps. */
@Composable
private fun RowScope.CenterActionSlot(action: NavBarAction) {
    Box(
        Modifier
            .weight(1f)
            // The bar's own height; fillMaxHeight would stretch the bar over the whole screen.
            .height(BAR_HEIGHT)
            .clickable(role = Role.Button, onClick = action.onClick),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            action.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Lines the label up with the destinations' labels.
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun RaisedActionButton(
    action: NavBarAction,
    modifier: Modifier = Modifier,
) {
    // A ring in the screen colour, so the button looks cut out of the bar's edge. It is a disc of its own rather
    // than a border: a border over the coloured disc leaves a faint fringe at its outer edge.
    Box(
        modifier
            .size(ACTION_SIZE)
            .background(MaterialTheme.colorScheme.background, CircleShape)
            .padding(4.dp)
            // The slot below already announces the action; this is the same target, made bigger.
            .clearAndSetSemantics {}
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = action.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            action.icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** Selected item in the brand colour on a pale pill; the rest in the muted text colour. */
@Composable
private fun navItemColors(): NavigationBarItemColors {
    val scheme = MaterialTheme.colorScheme
    return NavigationBarItemDefaults.colors(
        selectedIconColor = scheme.onPrimaryContainer,
        selectedTextColor = scheme.onBackground,
        indicatorColor = scheme.primaryContainer,
        unselectedIconColor = scheme.onSurfaceVariant,
        unselectedTextColor = scheme.onSurfaceVariant,
    )
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
        Column {
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
            Box(Modifier.fillMaxWidth().height(16.dp))
            AppNavigationBar(
                items = listOf(NavBarItem("Home", Icons.Filled.Home), NavBarItem("Profile", Icons.Filled.Person)),
                selectedIndex = 0,
                onSelect = {},
                centerAction = NavBarAction("New bill", AppIcons.ListAdd) {},
            )
        }
    }
}
