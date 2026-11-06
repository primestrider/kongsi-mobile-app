package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.R
import com.primestrider.kotlinstarter.core.designsystem.theme.AppThemeTokens
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews

enum class BannerVariant { INFO, SUCCESS, WARNING, ERROR }

/** Inline alert: icon, single-line title, wrapping message, optional action and dismiss button. */
@Composable
fun Banner(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    variant: BannerVariant = BannerVariant.INFO,
    onDismiss: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val (icon, accent) = variant.style()
    Surface(
        shape = RoundedCornerShape(Radii.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, accent),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.s3), horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            Icon(icon, contentDescription = null, tint = accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                action?.invoke()
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.component_dismiss, title))
                }
            }
        }
    }
}

@Composable
private fun BannerVariant.style(): Pair<ImageVector, Color> =
    when (this) {
        BannerVariant.INFO -> Icons.Filled.Info to AppThemeTokens.colors.info
        BannerVariant.SUCCESS -> Icons.Filled.CheckCircle to AppThemeTokens.colors.success
        BannerVariant.WARNING -> Icons.Filled.Warning to AppThemeTokens.colors.warning
        BannerVariant.ERROR -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
    }

@ThemePreviews
@Composable
private fun BannerPreview() {
    PreviewTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BannerVariant.entries.forEach { variant ->
                Banner(title = variant.name, message = "Something worth knowing.", variant = variant, onDismiss = {})
            }
        }
    }
}
