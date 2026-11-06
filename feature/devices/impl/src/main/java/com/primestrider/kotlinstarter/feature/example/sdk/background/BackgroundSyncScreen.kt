package com.primestrider.kotlinstarter.feature.example.sdk.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.component.SelectField
import com.primestrider.kotlinstarter.core.designsystem.component.SelectOption
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.feature.devices.impl.BuildConfig
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import java.text.DateFormat
import java.util.Date

/** Periodic WorkManager sync: on/off, interval, a debug-only "Run now", and what the worker recorded. */
@Composable
fun BackgroundSyncScreen(
    onBack: () -> Unit,
    viewModel: BackgroundSyncViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExamplePageScaffold(title = R.string.sdk_background_title, subtitle = R.string.sdk_background_subtitle, onBack = onBack) {
        BackgroundSyncContent(
            state = state,
            showRunNow = BuildConfig.DEBUG,
            onEnabled = viewModel::setEnabled,
            onInterval = viewModel::setInterval,
            onRefresh = viewModel::refresh,
            onRunNow = viewModel::runNow,
        )
    }
}

@Composable
internal fun BackgroundSyncContent(
    state: BackgroundSyncUiState,
    showRunNow: Boolean,
    onEnabled: (Boolean) -> Unit,
    onInterval: (Int) -> Unit,
    onRefresh: () -> Unit,
    onRunNow: () -> Unit,
) {
    val locale = currentLocale()
    SdkSection(R.string.sdk_background_sync_title, R.string.sdk_background_sync_description) {
        SwitchRow(stringResource(R.string.sdk_background_switch), checked = state.registered, onCheckedChange = onEnabled)
        SelectField(
            label = stringResource(R.string.sdk_background_interval),
            options = SYNC_INTERVALS.map { SelectOption(it, intervalLabel(it)) },
            selected = state.intervalMinutes,
            onSelect = onInterval,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            AppButton(stringResource(R.string.sdk_background_refresh), onClick = onRefresh, variant = ButtonVariant.OUTLINED)
            if (showRunNow) AppButton(stringResource(R.string.sdk_background_run_now), onClick = onRunNow, variant = ButtonVariant.OUTLINED)
        }
        if (showRunNow) Caption(stringResource(R.string.sdk_background_run_now_caption))
        Caption(stringResource(R.string.sdk_background_minimum_note))
    }
    SdkSection(R.string.sdk_background_status_title, R.string.sdk_background_status_description) {
        InfoCard(
            listOf(
                stringResource(R.string.sdk_background_info_status) to state.status?.let { stringResource(statusLabel(it)) },
                stringResource(R.string.sdk_background_info_registered) to
                    stringResource(if (state.registered) R.string.sdk_background_yes else R.string.sdk_background_no),
                stringResource(R.string.sdk_background_info_last_run) to
                    (
                        state.lastRun?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(it)) }
                            ?: stringResource(R.string.sdk_background_never)
                    ),
                stringResource(R.string.sdk_background_info_runs) to state.runs.toString(),
            ),
        )
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun intervalLabel(minutes: Int): String =
    when (minutes) {
        60 -> stringResource(R.string.sdk_background_every_hour)
        else -> stringResource(R.string.sdk_background_every_minutes, minutes)
    }

private fun statusLabel(state: WorkInfo.State): Int =
    when (state) {
        WorkInfo.State.ENQUEUED -> R.string.sdk_background_state_enqueued
        WorkInfo.State.RUNNING -> R.string.sdk_background_state_running
        WorkInfo.State.SUCCEEDED -> R.string.sdk_background_state_succeeded
        WorkInfo.State.FAILED -> R.string.sdk_background_state_failed
        WorkInfo.State.BLOCKED -> R.string.sdk_background_state_blocked
        WorkInfo.State.CANCELLED -> R.string.sdk_background_state_cancelled
    }

@ThemePreviews
@Composable
private fun BackgroundSyncScreenPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_background_title, subtitle = R.string.sdk_background_subtitle, onBack = {}) {
            BackgroundSyncContent(
                state = BackgroundSyncUiState(registered = true, status = WorkInfo.State.ENQUEUED, lastRun = 1_700_000_000_000L, runs = 3),
                showRunNow = true,
                onEnabled = {},
                onInterval = {},
                onRefresh = {},
                onRunNow = {},
            )
        }
    }
}
