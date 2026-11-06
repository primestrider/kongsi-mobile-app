package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews

/** Visual style of [AppButton], mapped to Material 3 button types. */
enum class ButtonVariant { PRIMARY, SECONDARY, OUTLINED, TEXT, DESTRUCTIVE }

/**
 * Button with a built-in loading state: while [loading] it shows a spinner, reports disabled,
 * and ignores clicks. Set [loading] synchronously in the click handler (before any suspend call) so a
 * fast double tap cannot run the action twice.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    fullWidth: Boolean = false,
) {
    val active = enabled && !loading
    val click = { if (active) onClick() }
    val sized = if (fullWidth) modifier.fillMaxWidth() else modifier
    val hasIcon = loading || leadingIcon != null
    val padding = if (hasIcon) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding
    val textPadding = if (hasIcon) ButtonDefaults.TextButtonWithIconContentPadding else ButtonDefaults.TextButtonContentPadding
    val content: @Composable RowScope.() -> Unit = { ButtonContent(text, loading, leadingIcon) }
    when (variant) {
        ButtonVariant.PRIMARY -> Button(click, sized, active, contentPadding = padding, content = content)
        ButtonVariant.SECONDARY -> FilledTonalButton(click, sized, active, contentPadding = padding, content = content)
        ButtonVariant.OUTLINED -> OutlinedButton(click, sized, active, contentPadding = padding, content = content)
        ButtonVariant.TEXT -> TextButton(click, sized, active, contentPadding = textPadding, content = content)
        ButtonVariant.DESTRUCTIVE ->
            Button(
                onClick = click,
                modifier = sized,
                enabled = active,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                contentPadding = padding,
                content = content,
            )
    }
}

@Composable
private fun ButtonContent(
    text: String,
    loading: Boolean,
    leadingIcon: ImageVector?,
) {
    when {
        loading -> {
            CircularProgressIndicator(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        }
        leadingIcon != null -> {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        }
    }
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@ThemePreviews
@Composable
private fun AppButtonPreview() {
    PreviewTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ButtonVariant.entries.forEach { variant ->
                AppButton(text = variant.name, onClick = {}, variant = variant)
            }
            AppButton(text = "With icon", onClick = {}, leadingIcon = Icons.Filled.Add)
            AppButton(text = "Loading", onClick = {}, loading = true)
            AppButton(text = "Disabled", onClick = {}, enabled = false)
            AppButton(text = "Full width", onClick = {}, fullWidth = true)
        }
    }
}
