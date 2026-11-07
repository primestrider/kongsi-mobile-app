package com.primestrider.kongsi.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.InfoRow
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.feature.profile.impl.R

/** What Kongsi is, the installed version, and where the user's data lives. */
@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
) {
    AppScaffold(title = stringResource(R.string.profile_section_about), onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            Text(stringResource(R.string.profile_about_body), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.profile_about_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InfoRow(label = stringResource(R.string.profile_version), value = versionName)
        }
    }
}

@ThemePreviews
@Composable
private fun AboutScreenPreview() {
    PreviewTheme {
        AboutScreen(versionName = "1.0.0", onBack = {})
    }
}
