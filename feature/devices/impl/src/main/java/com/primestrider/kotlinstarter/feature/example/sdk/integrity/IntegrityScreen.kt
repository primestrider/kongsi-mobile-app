package com.primestrider.kotlinstarter.feature.example.sdk.integrity

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection

/** The protected action the demo asks a token for; a real app hashes the request it is about to send. */
private const val SAMPLE_ACTION = "transfer:amount=50000"

/** Play Integrity (standard requests): a token bound to one action, for your server to verify. */
@Composable
fun IntegrityScreen(
    onBack: () -> Unit,
    viewModel: IntegrityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    IntegrityContent(state = state, onRequest = { viewModel.request(SAMPLE_ACTION) }, onBack = onBack)
}

@Composable
internal fun IntegrityContent(
    state: IntegrityState,
    onRequest: () -> Unit,
    onBack: () -> Unit,
) {
    ExamplePageScaffold(title = R.string.sdk_integrity_title, subtitle = R.string.sdk_integrity_subtitle, onBack = onBack) {
        SdkSection(R.string.sdk_integrity_section, R.string.sdk_integrity_section_description) {
            when (state) {
                IntegrityState.NotConfigured ->
                    Text(stringResource(R.string.sdk_integrity_not_configured), style = MaterialTheme.typography.bodyMedium)
                IntegrityState.Ready, IntegrityState.Requesting ->
                    AppButton(
                        stringResource(R.string.sdk_integrity_request),
                        onClick = onRequest,
                        loading = state == IntegrityState.Requesting,
                    )
                is IntegrityState.Token -> {
                    InfoCard(
                        listOf(
                            stringResource(R.string.sdk_integrity_action) to SAMPLE_ACTION,
                            stringResource(R.string.sdk_integrity_token) to state.preview,
                            stringResource(R.string.sdk_integrity_length) to state.length.toString(),
                        ),
                    )
                    Text(stringResource(R.string.sdk_integrity_verify_on_server), style = MaterialTheme.typography.bodySmall)
                    AppButton(stringResource(R.string.sdk_integrity_request), onClick = onRequest)
                }
                is IntegrityState.Failed -> {
                    Text(stringResource(R.string.sdk_integrity_failed, state.message), style = MaterialTheme.typography.bodyMedium)
                    AppButton(stringResource(R.string.sdk_integrity_request), onClick = onRequest)
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun IntegrityScreenPreview() {
    PreviewTheme {
        IntegrityContent(state = IntegrityState.Token(preview = "eyJhbGciOiJBMjU2S1ciLCJl…", length = 1180), onRequest = {}, onBack = {})
    }
}
