package com.primestrider.kotlinstarter.core.crash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.InfoRow
import com.primestrider.kotlinstarter.core.designsystem.component.SectionHeader
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.ui.R
import com.primestrider.kotlinstarter.core.util.currentLocale
import java.text.DateFormat
import java.util.Date

/** Shown instead of the app on the launch after a crash; [onContinue] clears the report and opens the app. */
@Composable
fun CrashFallbackScreen(
    report: CrashReport,
    onContinue: () -> Unit,
) {
    val locale = currentLocale()
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(Spacing.s12),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(stringResource(R.string.crash_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.crash_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Column {
                SectionHeader(stringResource(R.string.crash_details))
                InfoRow(stringResource(R.string.crash_type), report.exceptionClass.substringAfterLast('.'))
                InfoRow(
                    stringResource(R.string.crash_time),
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(report.timestamp)),
                )
            }
            AppButton(stringResource(R.string.crash_continue), onClick = onContinue, fullWidth = true)
        }
    }
}

@ThemePreviews
@Composable
private fun CrashFallbackScreenPreview() {
    PreviewTheme {
        CrashFallbackScreen(
            report = CrashReport(exceptionClass = "java.lang.IllegalStateException", message = "Boom", timestamp = 1_700_000_000_000L),
            onContinue = {},
        )
    }
}
