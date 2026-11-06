package com.primestrider.kotlinstarter.feature.example.sdk.appupdate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection

/** Play in-app updates (flexible): check, download in the background, install when the user chooses. */
@Composable
fun AppUpdateScreen(
    onBack: () -> Unit,
    viewModel: AppUpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Play's confirmation dialog is started for a result; the outcome arrives through the install listener.
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            viewModel.onUpdateFlowResult(it.resultCode)
        }
    AppUpdateContent(
        state = state,
        onCheck = viewModel::check,
        onUpdate = { viewModel.startUpdate(launcher) },
        onInstall = viewModel::completeUpdate,
        onBack = onBack,
    )
}

@Composable
internal fun AppUpdateContent(
    state: AppUpdateState,
    onCheck: () -> Unit,
    onUpdate: () -> Unit,
    onInstall: () -> Unit,
    onBack: () -> Unit,
) {
    ExamplePageScaffold(title = R.string.sdk_app_update_title, subtitle = R.string.sdk_app_update_subtitle, onBack = onBack) {
        SdkSection(R.string.sdk_app_update_section, R.string.sdk_app_update_section_description) {
            Text(
                when (state) {
                    AppUpdateState.Checking -> stringResource(R.string.sdk_app_update_checking)
                    AppUpdateState.UpToDate -> stringResource(R.string.sdk_app_update_up_to_date)
                    is AppUpdateState.Available -> stringResource(R.string.sdk_app_update_available, state.versionCode)
                    is AppUpdateState.Downloading -> stringResource(R.string.sdk_app_update_downloading)
                    AppUpdateState.Downloaded -> stringResource(R.string.sdk_app_update_downloaded)
                    is AppUpdateState.Failed -> stringResource(R.string.sdk_app_update_failed, state.code)
                    is AppUpdateState.Unavailable -> stringResource(R.string.sdk_app_update_unavailable)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (state is AppUpdateState.Downloading) {
                LinearProgressIndicator(
                    progress = { if (state.total > 0) state.bytes.toFloat() / state.total else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state is AppUpdateState.Unavailable) {
                Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when (state) {
                is AppUpdateState.Available -> AppButton(stringResource(R.string.sdk_app_update_update), onClick = onUpdate)
                AppUpdateState.Downloaded -> AppButton(stringResource(R.string.sdk_app_update_install), onClick = onInstall)
                AppUpdateState.Checking, is AppUpdateState.Downloading -> Unit
                else -> AppButton(stringResource(R.string.sdk_app_update_check), onClick = onCheck, variant = ButtonVariant.OUTLINED)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AppUpdateScreenPreview() {
    PreviewTheme {
        AppUpdateContent(state = AppUpdateState.Downloading(bytes = 3_000, total = 10_000), onCheck = {
        }, onUpdate = {}, onInstall = {}, onBack = {})
    }
}
