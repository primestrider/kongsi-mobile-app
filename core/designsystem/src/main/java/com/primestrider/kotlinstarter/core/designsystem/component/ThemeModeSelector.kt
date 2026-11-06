package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.designsystem.R
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.model.ThemeMode

/** System / Light / Dark segmented control shared by Settings and the Theme showcase page. */
@Composable
fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = ThemeMode.entries.associateWith { stringResource(it.labelRes()) }
    SegmentedChoice(ThemeMode.entries, selected, onSelect, label = { labels.getValue(it) }, modifier = modifier)
}

@StringRes
private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.component_theme_system
        ThemeMode.LIGHT -> R.string.component_theme_light
        ThemeMode.DARK -> R.string.component_theme_dark
    }

@ThemePreviews
@Composable
private fun ThemeModeSelectorPreview() {
    PreviewTheme {
        ThemeModeSelector(selected = ThemeMode.SYSTEM, onSelect = {})
    }
}
