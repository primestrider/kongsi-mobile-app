package com.primestrider.kotlinstarter.feature.example.sdk.statusbar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.primestrider.kotlinstarter.core.designsystem.component.SelectField
import com.primestrider.kotlinstarter.core.designsystem.component.SelectOption
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.util.findActivity
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection

/** Status bar icon style and visibility for this screen only; leaving the screen restores both. */
@Composable
fun StatusBarScreen(onBack: () -> Unit) {
    var style by rememberSaveable { mutableStateOf(BarStyle.AUTO) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    ApplyStatusBar(appearanceLight = lightIcons(style, darkTheme), hidden = hidden, darkTheme = darkTheme)

    val options =
        listOf(
            SelectOption(BarStyle.AUTO, stringResource(R.string.sdk_status_bar_auto)),
            SelectOption(BarStyle.LIGHT_ICONS, stringResource(R.string.sdk_status_bar_light)),
            SelectOption(BarStyle.DARK_ICONS, stringResource(R.string.sdk_status_bar_dark)),
            SelectOption(BarStyle.INVERTED, stringResource(R.string.sdk_status_bar_inverted)),
        )
    ExamplePageScaffold(
        title = R.string.sdk_status_bar_title,
        subtitle = R.string.sdk_status_bar_subtitle,
        onBack = onBack,
    ) {
        SdkSection(R.string.sdk_status_bar_section, R.string.sdk_status_bar_section_description) {
            SelectField(
                label = stringResource(R.string.sdk_status_bar_style),
                options = options,
                selected = style,
                onSelect = { style = it },
            )
            SwitchRow(label = stringResource(R.string.sdk_status_bar_hidden), checked = hidden, onCheckedChange = { hidden = it })
            Text(
                stringResource(R.string.sdk_status_bar_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Applies the status bar look to the activity window ([appearanceLight] = dark icons) and puts the app's own
 * look back on leave: the AUTO value for [darkTheme], the same one MainActivity applies, not a snapshot that
 * could predate the app theme. The apply effect is a LaunchedEffect keyed on [darkTheme] too, so it runs after
 * (and wins over) MainActivity's own theme effect when the theme flips.
 */
@Composable
internal fun ApplyStatusBar(
    appearanceLight: Boolean,
    hidden: Boolean,
    darkTheme: Boolean,
) {
    val view = LocalView.current
    val window = LocalContext.current.findActivity()?.window ?: return
    val controller = remember(window, view) { WindowCompat.getInsetsController(window, view) }
    val currentDarkTheme by rememberUpdatedState(darkTheme)
    DisposableEffect(controller) {
        onDispose {
            controller.isAppearanceLightStatusBars = lightIcons(BarStyle.AUTO, currentDarkTheme)
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }
    LaunchedEffect(controller, appearanceLight, hidden, darkTheme) {
        controller.isAppearanceLightStatusBars = appearanceLight
        if (hidden) controller.hide(WindowInsetsCompat.Type.statusBars()) else controller.show(WindowInsetsCompat.Type.statusBars())
    }
}

@ThemePreviews
@Composable
private fun StatusBarScreenPreview() {
    PreviewTheme {
        StatusBarScreen(onBack = {})
    }
}
