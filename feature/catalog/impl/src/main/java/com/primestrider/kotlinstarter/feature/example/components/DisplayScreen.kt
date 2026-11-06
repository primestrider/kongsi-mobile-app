package com.primestrider.kotlinstarter.feature.example.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.primestrider.kotlinstarter.core.designsystem.component.AppSwitch
import com.primestrider.kotlinstarter.core.designsystem.component.Avatar
import com.primestrider.kotlinstarter.core.designsystem.component.AvatarGroup
import com.primestrider.kotlinstarter.core.designsystem.component.AvatarSize
import com.primestrider.kotlinstarter.core.designsystem.component.AvatarStatus
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.EmptyState
import com.primestrider.kotlinstarter.core.designsystem.component.InfoRow
import com.primestrider.kotlinstarter.core.designsystem.component.SectionHeader
import com.primestrider.kotlinstarter.core.designsystem.component.Skeleton
import com.primestrider.kotlinstarter.core.designsystem.component.SkeletonText
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.rememberSaveableStateList
import kotlin.math.roundToInt

internal const val PROGRESS_BAR_TAG = "display_progress"
internal const val LONG_ROW_SWITCH_TAG = "display_long_row_switch"
internal const val LONG_HEADLINE =
    "A very long row title that has to truncate rather than shove the trailing slot off screen"

private const val PROGRESS_STEP = 0.1f
private const val BADGE_MAX = 99

/** Material 3 display components, plus avatar / skeleton / empty-state / banner patterns M3 does not ship. */
@Composable
fun DisplayScreen(onBack: () -> Unit) {
    ExamplePageScaffold(
        title = R.string.example_components_display_title,
        subtitle = R.string.example_components_display_subtitle,
        onBack = onBack,
    ) {
        CardsSection()
        ListItemsSection()
        SwipeToDismissSection()
        PullToRefreshSection()
        BadgesSection()
        DividersSection()
        ProgressSection()
        AvatarsSection()
        SkeletonSection()
        HeaderInfoSection()
        EmptyStateSection()
        BannersSection()
    }
}

@Composable
private fun CardsSection() {
    var taps by rememberSaveable { mutableIntStateOf(0) }
    DemoSection(R.string.components_section_cards) {
        Card(Modifier.fillMaxWidth()) { CardText(R.string.components_card_filled) }
        ElevatedCard(Modifier.fillMaxWidth()) { CardText(R.string.components_card_elevated) }
        OutlinedCard(Modifier.fillMaxWidth()) { CardText(R.string.components_card_outlined) }
        OutlinedCard(onClick = { taps++ }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.components_card_clickable, taps), Modifier.padding(Spacing.s4))
        }
    }
}

@Composable
private fun CardText(
    @StringRes title: Int,
) {
    Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.components_card_body), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ListItemsSection() {
    var enabled by rememberSaveable { mutableStateOf(true) }
    DemoSection(R.string.components_section_list_items) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.components_list_profile)) },
            supportingContent = { Text(stringResource(R.string.components_list_profile_supporting)) },
            leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(LONG_HEADLINE, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
            trailingContent = {
                AppSwitch(checked = enabled, onCheckedChange = { enabled = it }, modifier = Modifier.testTag(LONG_ROW_SWITCH_TAG))
            },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.components_list_sign_out)) },
            colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
        )
    }
}

@Composable
private fun BadgesSection() {
    val count = 120
    DemoSection(R.string.components_section_badges) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s6), verticalAlignment = Alignment.CenterVertically) {
            Badge()
            Badge { Text("8") }
            BadgedBox(badge = { Badge { Text(if (count > BADGE_MAX) "$BADGE_MAX+" else "$count") } }) {
                Icon(Icons.Filled.Email, contentDescription = stringResource(R.string.components_cd_notifications))
            }
            BadgedBox(badge = { Badge() }) {
                Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.components_cd_notifications))
            }
        }
    }
}

@Composable
private fun DividersSection() {
    DemoSection(R.string.components_section_dividers) {
        HorizontalDivider()
        HorizontalDivider(Modifier.padding(start = Spacing.s12))
        Row(Modifier.height(Spacing.s8), verticalAlignment = Alignment.CenterVertically) {
            Text("A")
            VerticalDivider(Modifier.padding(horizontal = Spacing.s3))
            Text("B")
            VerticalDivider(Modifier.padding(horizontal = Spacing.s3))
            Text("C")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                stringResource(R.string.components_divider_label),
                Modifier.padding(horizontal = Spacing.s3),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProgressSection() {
    var progress by rememberSaveable { mutableFloatStateOf(0.4f) }
    DemoSection(R.string.components_section_progress) {
        Text(stringResource(R.string.components_progress_value, (progress * 100).roundToInt()))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().testTag(PROGRESS_BAR_TAG))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            OutlinedButton(onClick = { progress = (progress - PROGRESS_STEP).coerceIn(0f, 1f) }) { Text("−10%") }
            OutlinedButton(onClick = { progress = (progress + PROGRESS_STEP).coerceIn(0f, 1f) }) { Text("+10%") }
        }
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s4), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(progress = { progress })
            CircularProgressIndicator(Modifier.size(Spacing.s4), strokeWidth = Spacing.s0_5)
            CircularProgressIndicator(Modifier.size(Spacing.s8))
            CircularProgressIndicator(Modifier.size(Spacing.s12))
        }
    }
}

@Composable
private fun AvatarsSection() {
    val names = listOf("Ada Lovelace", "Grace Hopper", "Alan Turing", "Margaret Hamilton", "Linus Torvalds", "Barbara Liskov")
    DemoSection(R.string.components_section_avatars) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s4), verticalAlignment = Alignment.CenterVertically) {
            Avatar("Ada Lovelace", size = AvatarSize.SMALL)
            Avatar("Grace Hopper", size = AvatarSize.LARGE)
            Avatar("Alan Turing", size = AvatarSize.LARGE, shape = RoundedCornerShape(Radii.md))
            Avatar("Margaret Hamilton", size = AvatarSize.XLARGE, status = AvatarStatus.ONLINE)
        }
        AvatarGroup(names, max = 4)
    }
}

@Composable
private fun SkeletonSection() {
    var loaded by rememberSaveable { mutableStateOf(false) }
    DemoSection(R.string.components_section_skeleton) {
        if (loaded) {
            Text(stringResource(R.string.components_skeleton_content))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                Skeleton(Modifier.size(Spacing.s12), CircleShape)
                SkeletonText(lines = 2, modifier = Modifier.weight(1f))
            }
        }
        TextButton(onClick = { loaded = !loaded }) {
            Text(stringResource(if (loaded) R.string.components_skeleton_show else R.string.components_skeleton_loaded))
        }
    }
}

@Composable
private fun HeaderInfoSection() {
    DemoSection(R.string.components_section_header_info) {
        SectionHeader(
            title = stringResource(R.string.components_header_recent),
            actionLabel = stringResource(R.string.components_header_see_all),
            onAction = {},
        )
        InfoRow(stringResource(R.string.components_info_order), "#INV-2026-10-0042")
        InfoRow(stringResource(R.string.components_info_status), stringResource(R.string.components_info_status_value))
        InfoRow(stringResource(R.string.components_info_address), "Jl. Merdeka No. 17, Kebayoran Baru, Jakarta Selatan 12110")
    }
}

@Composable
private fun EmptyStateSection() {
    DemoSection(R.string.components_section_empty) {
        EmptyState(
            title = stringResource(R.string.components_empty_title),
            description = stringResource(R.string.components_empty_description),
            icon = Icons.Filled.Email,
            actionLabel = stringResource(R.string.components_empty_action),
            onAction = {},
        )
    }
}

private data class BannerDemo(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val variant: BannerVariant,
)

private val bannerDemos =
    listOf(
        BannerDemo(R.string.components_banner_info_title, R.string.components_banner_info_body, BannerVariant.INFO),
        BannerDemo(R.string.components_banner_success_title, R.string.components_banner_success_body, BannerVariant.SUCCESS),
        BannerDemo(R.string.components_banner_warning_title, R.string.components_banner_warning_body, BannerVariant.WARNING),
        BannerDemo(R.string.components_banner_error_title, R.string.components_banner_error_body, BannerVariant.ERROR),
    )

@Composable
private fun BannersSection() {
    val visible = rememberSaveableStateList(true, true, true, true)
    DemoSection(R.string.components_section_banners) {
        bannerDemos.forEachIndexed { index, banner ->
            if (visible[index]) {
                Banner(
                    title = stringResource(banner.title),
                    message = stringResource(banner.body),
                    variant = banner.variant,
                    onDismiss = { visible[index] = false },
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun DisplayScreenPreview() {
    PreviewTheme {
        DisplayScreen(onBack = {})
    }
}
