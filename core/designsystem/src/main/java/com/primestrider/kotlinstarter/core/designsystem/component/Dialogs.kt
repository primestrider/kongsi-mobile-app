package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.primestrider.kotlinstarter.core.designsystem.R
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews

/**
 * Confirmation modal. [destructive] colors the confirm action as an error; [dismissible] = false
 * ignores back and outside taps so only the buttons close it; [dismissLabel] = null hides the
 * dismiss button. The caller decides visibility by composing it or not.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = stringResource(R.string.component_cancel),
    icon: ImageVector? = null,
    destructive: Boolean = false,
    dismissible: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        modifier = modifier,
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = dismissLabel?.let { { TextButton(onClick = onDismiss) { Text(it) } } },
        properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible),
    )
}

@ThemePreviews
@Composable
private fun ConfirmDialogPreview() {
    PreviewTheme {
        ConfirmDialog(
            title = "Delete item?",
            message = "This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
            destructive = true,
        )
    }
}
