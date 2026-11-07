package com.primestrider.kongsi.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.icon.AppIcons
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews

private const val BADGE_MAX = 99

/** Diameter of the raised action button, centred on the bar's top edge. */
private val ACTION_SIZE: Dp = 60.dp

/** Height of a Material 3 navigation bar (without the system navigation bar inset). */
private val BAR_HEIGHT: Dp = 80.dp

/** Radius of the half-round bites along the bar's torn top edge. */
private val TEAR_RADIUS: Dp = 6.dp

/** Distance from one bite to the next. */
private val TEAR_PERIOD: Dp = 16.dp

/** One destination of [AppNavigationBar]; a positive [badgeCount] shows a count badge ("99+" above 99). */
data class NavBarItem(
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int? = null,
)

/**
 * The app's main action, shown as a round ink button on the middle of the bar's top edge and as a FAB at the top of the
 * navigation rail. It is an action, not a destination: it is never selected.
 */
data class NavBarAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

const val APP_NAVIGATION_BAR_TAG = "app_navigation_bar"

/**
 * Bottom navigation bar for top-level destinations, cut like a torn-off receipt: a card whose top edge is a row of
 * half-round bites. Stateless: the caller owns [selectedIndex]. With a [centerAction] the destinations split around a
 * round ink button sitting on the middle of the edge (the first half of [items] on the start side).
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
    // The raised button's upper half sits in a strip above the bar: drawn outside the bar it would be clipped and,
    // past the bar's bounds, would not receive touches.
    Box(modifier.testTag(APP_NAVIGATION_BAR_TAG)) {
        Surface(
            shape = TornEdgeShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 6.dp,
            modifier = Modifier.padding(top = if (centerAction != null) ACTION_SIZE / 2 else 0.dp),
        ) {
            // Room for the bites, so the items stay on solid card.
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.padding(top = TEAR_RADIUS),
            ) {
                items.forEachIndexed { index, item ->
                    if (centerAction != null && index == split) CenterSlot()
                    NavigationBarItem(
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        icon = { NavBarIcon(item) },
                        label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = navItemColors(),
                    )
                }
                if (centerAction != null && split == items.size) CenterSlot()
            }
        }
        if (centerAction != null) RaisedActionButton(centerAction, Modifier.align(Alignment.TopCenter))
    }
}

/** The space in the bar under the raised button, so the destinations keep clear of it. */
@Composable
private fun RowScope.CenterSlot() {
    Box(Modifier.weight(1f).height(BAR_HEIGHT))
}

/** The round main action on the bar's top edge. It has no visible label; TalkBack reads [NavBarAction.label]. */
@Composable
private fun RaisedActionButton(
    action: NavBarAction,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(ACTION_SIZE)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(role = Role.Button, onClick = action.onClick)
            .semantics { contentDescription = action.label },
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

/**
 * A rectangle whose top edge is torn like a receipt: half-round bites of [TEAR_RADIUS] every [TEAR_PERIOD], centred
 * so both ends of the edge match.
 */
private object TornEdgeShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = with(density) { TEAR_RADIUS.toPx() }
        val period = with(density) { TEAR_PERIOD.toPx() }
        val count = (size.width / period).toInt()
        val offset = (size.width - count * period) / 2
        val path =
            Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, 0f)
                repeat(count) { i ->
                    val center = offset + period / 2 + i * period
                    lineTo(center - radius, 0f)
                    arcTo(
                        rect = Rect(center - radius, -radius, center + radius, radius),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false,
                    )
                }
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                close()
            }
        return Outline.Generic(path)
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
