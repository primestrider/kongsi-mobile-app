package com.primestrider.kotlinstarter.feature.example.sdk

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.designsystem.component.AppScaffold
import com.primestrider.kotlinstarter.core.designsystem.component.EmptyState
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection

/** Resolves [id] against [sdkDemos]; an unknown id shows an empty state with a back arrow. */
@Composable
fun SdkDemoScreen(
    id: String,
    onBack: () -> Unit,
) {
    val demo = sdkDemos.firstOrNull { it.id == id }
    if (demo != null) {
        demo.screen(onBack)
        return
    }
    AppScaffold(title = stringResource(R.string.example_home_sdk_title), onBack = onBack) { padding ->
        EmptyState(title = stringResource(R.string.sdk_unknown), modifier = Modifier.fillMaxSize().padding(padding))
    }
}

/** A [DemoSection] with a muted one-line description under its title, as every SDK demo uses it. */
@Composable
internal fun SdkSection(
    @StringRes title: Int,
    @StringRes description: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    DemoSection(title) {
        Text(
            stringResource(description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) { content() }
    }
}

@ThemePreviews
@Composable
private fun SdkDemoScreenPreview() {
    PreviewTheme {
        SdkDemoScreen(id = "unknown", onBack = {})
    }
}
