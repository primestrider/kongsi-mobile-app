package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.primestrider.kotlinstarter.core.designsystem.R
import com.primestrider.kotlinstarter.core.designsystem.theme.AppThemeTokens
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews

enum class AvatarSize(
    val dp: Dp,
) {
    SMALL(32.dp),
    MEDIUM(40.dp),
    LARGE(48.dp),
    XLARGE(64.dp),
}

enum class AvatarStatus(
    @StringRes val label: Int,
) {
    ONLINE(R.string.component_status_online),
    OFFLINE(R.string.component_status_offline),
    BUSY(R.string.component_status_busy),
    AWAY(R.string.component_status_away),
}

/** First letter of the first two words, uppercased; `"?"` when the name is blank. */
fun initialsOf(name: String): String =
    name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

/**
 * Avatar with an optional presence dot; announced as the name (plus status). Shows the initials, or the image at
 * [imageUrl] once it has loaded (initials stay while loading and when loading fails).
 */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.MEDIUM,
    shape: Shape = CircleShape,
    status: AvatarStatus? = null,
    imageUrl: String? = null,
) {
    val description = status?.let { "$name, ${stringResource(it.label)}" } ?: name
    Box(modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        val initials = initialsOf(name)
        if (imageUrl == null) {
            AvatarCircle(initials, size, shape)
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(size.dp).clip(shape),
                contentScale = ContentScale.Crop,
                loading = { AvatarCircle(initials, size, shape) },
                error = { AvatarCircle(initials, size, shape) },
            )
        }
        if (status != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size.dp / 4)
                    .background(status.color(), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
    }
}

/** Overlapping avatars; names beyond [max] collapse into a "+N" chip. */
@Composable
fun AvatarGroup(
    names: List<String>,
    modifier: Modifier = Modifier,
    max: Int = 4,
    size: AvatarSize = AvatarSize.MEDIUM,
) {
    val shown = names.take(max.coerceAtLeast(0))
    val hidden = names.size - shown.size
    val ring = Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
    val more = stringResource(R.string.component_avatar_more, hidden)
    // Negative spacing overlaps the avatars and keeps the group's measured width tight.
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(-size.dp / 4)) {
        shown.forEach { name -> Avatar(name, ring, size) }
        if (hidden > 0) {
            AvatarCircle("+$hidden", size, CircleShape, ring.semantics { contentDescription = more })
        }
    }
}

@Composable
private fun AvatarCircle(
    label: String,
    size: AvatarSize,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(size.dp).background(MaterialTheme.colorScheme.primaryContainer, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = size.textStyle(), color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun AvatarStatus.color(): Color =
    when (this) {
        AvatarStatus.ONLINE -> AppThemeTokens.colors.success
        AvatarStatus.OFFLINE -> MaterialTheme.colorScheme.outline
        AvatarStatus.BUSY -> MaterialTheme.colorScheme.error
        AvatarStatus.AWAY -> AppThemeTokens.colors.warning
    }

@Composable
private fun AvatarSize.textStyle(): TextStyle =
    when (this) {
        AvatarSize.SMALL -> MaterialTheme.typography.labelSmall
        AvatarSize.MEDIUM -> MaterialTheme.typography.labelLarge
        AvatarSize.LARGE -> MaterialTheme.typography.titleSmall
        AvatarSize.XLARGE -> MaterialTheme.typography.titleLarge
    }

@ThemePreviews
@Composable
private fun AvatarPreview() {
    PreviewTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvatarSize.entries.forEach { Avatar(name = "Ada Lovelace", size = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvatarStatus.entries.forEach { Avatar(name = "Grace Hopper", status = it) }
            }
            AvatarGroup(names = listOf("Ada Lovelace", "Grace Hopper", "Alan Turing", "Linus Torvalds", "Margaret Hamilton"))
        }
    }
}
