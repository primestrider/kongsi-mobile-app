package com.primestrider.kotlinstarter.feature.example.sdk.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.component.SelectField
import com.primestrider.kotlinstarter.core.designsystem.component.SelectOption
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.permission.PermissionGate
import com.primestrider.kotlinstarter.core.permission.permissionRequested
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.core.util.findActivity
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection

private val DELAYS =
    listOf(
        0 to R.string.sdk_notifications_delay_now,
        5 to R.string.sdk_notifications_delay_5s,
        30 to R.string.sdk_notifications_delay_30s,
        60 to R.string.sdk_notifications_delay_1m,
    )

/**
 * Where the notification permission stands; [GRANTED_LEGACY] is Android 12 and below, where nothing has to be asked.
 * There, [DENIED] means the user turned the app's notifications off in Settings.
 */
enum class NotificationStatus(
    val label: Int,
    val granted: Boolean,
) {
    GRANTED(R.string.sdk_notifications_granted, true),
    GRANTED_LEGACY(R.string.sdk_notifications_granted_legacy, true),
    DENIED(R.string.sdk_notifications_denied, false),
    NOT_REQUESTED(R.string.sdk_notifications_not_requested, false),
}

internal fun notificationStatus(context: Context): NotificationStatus =
    when {
        // Nothing to ask before Android 13, but the user can still turn the app's notifications off in Settings.
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
            if (NotificationManagerCompat
                    .from(
                        context,
                    ).areNotificationsEnabled()
            ) {
                NotificationStatus.GRANTED_LEGACY
            } else {
                NotificationStatus.DENIED
            }
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ->
            NotificationStatus.GRANTED
        permissionRequested(context, Manifest.permission.POST_NOTIFICATIONS) -> NotificationStatus.DENIED
        else -> NotificationStatus.NOT_REQUESTED
    }

/** Local notifications: right now or after a delay, shown or held back while the app is open, with a log of what happened. */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val scheduler = remember(context) { NotificationScheduler(context) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf(notificationStatus(context)) }
    var pending by remember { mutableIntStateOf(scheduler.pending()) }
    var showInForeground by remember { mutableStateOf(scheduler.showInForeground) }
    val tappedLabel by rememberUpdatedState(stringResource(R.string.sdk_notifications_event_tapped))
    val receivedLabel by rememberUpdatedState(stringResource(R.string.sdk_notifications_event_received))
    val currentLocale by rememberUpdatedState(locale)
    val scheduledLog = stringResource(R.string.sdk_notifications_log_scheduled, state.delaySeconds)
    val sentLog = stringResource(R.string.sdk_notifications_log_sent)
    val cancelledLog = stringResource(R.string.sdk_notifications_log_cancelled)

    LaunchedEffect(scheduler) { scheduler.ensureChannel() }
    LaunchedEffect(scheduler) {
        NotificationEvents.received.collect { title ->
            viewModel.log(receivedLabel.format(title), currentLocale)
            pending = scheduler.pending()
        }
    }
    // Back from the system dialog or from Settings: the permission may have changed.
    LifecycleResumeEffect(scheduler) {
        status = notificationStatus(context)
        pending = scheduler.pending()
        onPauseOrDispose {}
    }
    // A tap on a notification arrives as the activity's intent (cold start) or as a new intent (singleTop).
    DisposableEffect(context) {
        val activity = context.findActivity() as? ComponentActivity

        fun handle(intent: Intent) {
            val (id, title) = NotificationScheduler.tapOf(intent) ?: return
            // The launch intent stays on the activity: report each notification once per process.
            if (NotificationEvents.firstTap(id)) viewModel.onTapped(id, tappedLabel.format(title), currentLocale)
        }
        val listener = Consumer<Intent> { handle(it) }
        activity?.let {
            handle(it.intent)
            it.addOnNewIntentListener(listener)
        }
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

    ExamplePageScaffold(
        title = R.string.sdk_notifications_title,
        subtitle = R.string.sdk_notifications_subtitle,
        onBack = onBack,
    ) {
        NotificationsContent(
            state = state,
            status = status,
            pending = pending,
            showInForeground = showInForeground,
            onTitle = viewModel::setTitle,
            onDelay = viewModel::setDelay,
            onShowInForeground = {
                showInForeground = it
                scheduler.showInForeground = it
            },
            onSchedule = {
                scheduler.schedule(state.title, notificationBody(context, state.delaySeconds), state.delaySeconds)
                viewModel.log(if (state.delaySeconds > 0) scheduledLog else sentLog, locale)
                pending = scheduler.pending()
            },
            onCancelAll = {
                scheduler.cancelAll()
                viewModel.log(cancelledLog, locale)
                pending = scheduler.pending()
            },
        )
    }
}

/**
 * The sections of the demo for a given state. Everything shows before the permission is granted; the gate under the
 * Status row asks for it, and "Schedule" stays disabled until [status] is granted.
 */
@Composable
internal fun NotificationsContent(
    state: NotificationsUiState,
    status: NotificationStatus,
    pending: Int,
    showInForeground: Boolean,
    onTitle: (String) -> Unit,
    onDelay: (Int) -> Unit,
    onShowInForeground: (Boolean) -> Unit,
    onSchedule: () -> Unit,
    onCancelAll: () -> Unit,
) {
    SdkSection(R.string.sdk_notifications_permission_title, R.string.sdk_notifications_permission_description) {
        InfoCard(listOf(stringResource(R.string.sdk_notifications_status) to stringResource(status.label)))
        // Shows the "Grant access" / "Open Settings" card until granted; nothing once it is.
        PermissionGate(
            permissions = notificationPermissions(),
            reason = stringResource(R.string.sdk_notifications_reason),
        ) {}
    }
    SdkSection(R.string.sdk_notifications_local_title, R.string.sdk_notifications_local_description) {
        AppTextField(
            value = state.title,
            onValueChange = onTitle,
            label = stringResource(R.string.sdk_notifications_field_title),
        )
        SelectField(
            label = stringResource(R.string.sdk_notifications_delay),
            options = DELAYS.map { (seconds, label) -> SelectOption(seconds, stringResource(label)) },
            selected = state.delaySeconds,
            onSelect = onDelay,
        )
        SwitchRow(
            label = stringResource(R.string.sdk_notifications_foreground),
            checked = showInForeground,
            onCheckedChange = onShowInForeground,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            AppButton(stringResource(R.string.sdk_notifications_schedule), onClick = onSchedule, enabled = status.granted)
            AppButton(stringResource(R.string.sdk_notifications_cancel_all), onClick = onCancelAll, variant = ButtonVariant.OUTLINED)
        }
        Text(
            stringResource(R.string.sdk_notifications_pending, pending),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    SdkSection(R.string.sdk_notifications_events_title, R.string.sdk_notifications_events_description) {
        if (state.events.isEmpty()) {
            Text(stringResource(R.string.sdk_notifications_events_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            state.events.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

/** POST_NOTIFICATIONS is a runtime permission from Android 13; before that there is nothing to ask for. */
private fun notificationPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()

@ThemePreviews
@Composable
private fun NotificationsScreenPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_notifications_title, subtitle = R.string.sdk_notifications_subtitle, onBack = {}) {
            NotificationsContent(
                state = NotificationsUiState(),
                status = NotificationStatus.NOT_REQUESTED,
                pending = 0,
                showInForeground = true,
                onTitle = {},
                onDelay = {},
                onShowInForeground = {},
                onSchedule = {},
                onCancelAll = {},
            )
        }
    }
}
