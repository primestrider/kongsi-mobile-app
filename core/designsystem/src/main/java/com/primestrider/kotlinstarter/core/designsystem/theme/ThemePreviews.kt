package com.primestrider.kotlinstarter.core.designsystem.theme

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.primestrider.kotlinstarter.core.designsystem.component.LocalMessenger
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger

/**
 * Shows a preview twice, in light and dark theme. Put it on a private `@Composable` at the end of a screen or component
 * file and wrap the content in [PreviewTheme]. `PreviewCoverageTest` requires one per screen and component file.
 */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
annotation class ThemePreviews

/** [AppTheme] for the preview's light/dark mode, on a surface, with the app-wide values screens read (a [Messenger]). */
@Composable
fun PreviewTheme(content: @Composable () -> Unit) {
    AppTheme(darkTheme = isSystemInDarkTheme()) {
        CompositionLocalProvider(LocalMessenger provides remember { Messenger() }) {
            Surface(content = content)
        }
    }
}
