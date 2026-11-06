package com.primestrider.kotlinstarter.core.designsystem.component

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.designsystem.theme.AppThemeTokens
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Text that is either ready ([Plain]) or a string resource resolved in the current app language ([Resource]). */
sealed interface UiText {
    data class Plain(
        val value: String,
    ) : UiText

    data class Resource(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
}

fun UiText.resolve(resources: Resources): String =
    when (this) {
        is UiText.Plain -> value
        is UiText.Resource -> resources.getString(id, *args.toTypedArray())
    }

@Composable
fun UiText.asString(): String =
    when (this) {
        is UiText.Plain -> value
        is UiText.Resource -> stringResource(id, *args.toTypedArray())
    }

enum class MessageVariant { INFO, SUCCESS, WARNING, ERROR }

/** A transient message; [onAction] runs if the user taps [actionLabel]. */
data class UiMessage(
    val text: UiText,
    val variant: MessageVariant = MessageVariant.INFO,
    val actionLabel: UiText? = null,
    val onAction: (() -> Unit)? = null,
    val long: Boolean = false,
)

/**
 * App-wide message queue. Inject it into a ViewModel, or read [LocalMessenger] in a composable.
 * A buffered channel delivers each message once and keeps messages sent before the UI collects
 * (e.g. during rotation). Up to 64 messages are buffered; beyond that the newest are dropped.
 * Keep [UiMessage.onAction] lambdas short-lived: they live until the message is shown.
 */
@Singleton
class Messenger
    @Inject
    constructor() {
        private val channel = Channel<UiMessage>(Channel.BUFFERED)
        val messages: Flow<UiMessage> = channel.receiveAsFlow()

        fun show(message: UiMessage) {
            channel.trySend(message)
        }

        fun info(text: UiText) = show(UiMessage(text, MessageVariant.INFO))

        fun success(text: UiText) = show(UiMessage(text, MessageVariant.SUCCESS))

        fun warning(text: UiText) = show(UiMessage(text, MessageVariant.WARNING))

        fun error(text: UiText) = show(UiMessage(text, MessageVariant.ERROR))
    }

val LocalMessenger = staticCompositionLocalOf<Messenger> { error("No Messenger provided") }

private data class MessageVisuals(
    override val message: String,
    override val actionLabel: String?,
    override val duration: SnackbarDuration,
    val variant: MessageVariant,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
}

/**
 * Collects [messenger] and shows each message as a snackbar, one after another in the order sent
 * ([SnackbarHostState] queues them), so a burst of messages is never cut short.
 * Put it in the root `Scaffold`'s `snackbarHost` slot.
 */
@Composable
fun MessengerHost(
    messenger: Messenger,
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    LaunchedEffect(messenger, hostState) {
        messenger.messages.collect { message ->
            val visuals =
                MessageVisuals(
                    message = message.text.resolve(resources),
                    actionLabel = message.actionLabel?.resolve(resources),
                    duration = if (message.long) SnackbarDuration.Long else SnackbarDuration.Short,
                    variant = message.variant,
                )
            if (hostState.showSnackbar(visuals) == SnackbarResult.ActionPerformed) message.onAction?.invoke()
        }
    }
    SnackbarHost(hostState, modifier) { data -> VariantSnackbar(data) }
}

@Composable
private fun VariantSnackbar(data: SnackbarData) {
    val variant = (data.visuals as? MessageVisuals)?.variant ?: MessageVariant.INFO
    val (icon, accent) = variant.style()
    Snackbar(
        modifier = Modifier.padding(Spacing.s3),
        action =
            data.visuals.actionLabel?.let { label ->
                { TextButton(onClick = { data.performAction() }) { Text(label, color = MaterialTheme.colorScheme.inversePrimary) } }
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            Icon(icon, contentDescription = null, tint = accent)
            Text(data.visuals.message)
        }
    }
}

@Composable
private fun MessageVariant.style(): Pair<ImageVector, Color> =
    when (this) {
        MessageVariant.INFO -> Icons.Filled.Info to AppThemeTokens.colors.info
        MessageVariant.SUCCESS -> Icons.Filled.CheckCircle to AppThemeTokens.colors.success
        MessageVariant.WARNING -> Icons.Filled.Warning to AppThemeTokens.colors.warning
        MessageVariant.ERROR -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
    }

@ThemePreviews
@Composable
private fun MessengerHostPreview() {
    PreviewTheme {
        val messenger = remember { Messenger().apply { error(UiText.Plain("Could not save changes")) } }
        MessengerHost(messenger = messenger, hostState = remember { SnackbarHostState() })
    }
}
