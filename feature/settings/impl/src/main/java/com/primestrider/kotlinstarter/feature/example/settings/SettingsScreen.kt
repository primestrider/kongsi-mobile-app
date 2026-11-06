package com.primestrider.kotlinstarter.feature.example.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppRadioButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppScaffold
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.SectionHeader
import com.primestrider.kotlinstarter.core.designsystem.component.ThemeModeSelector
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.i18n.AppLanguage
import com.primestrider.kotlinstarter.core.model.ThemeMode
import com.primestrider.kotlinstarter.feature.settings.impl.BuildConfig
import com.primestrider.kotlinstarter.feature.settings.impl.R

const val SETTINGS_LIST_TAG = "settings_list"

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: (() -> Unit)?,
) {
    AppScaffold(title = stringResource(R.string.settings_title), onBack = onBack) { padding ->
        SettingsContent(state, onThemeModeChange, onLanguageChange, Modifier.padding(padding).consumeWindowInsets(padding))
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .testTag(SETTINGS_LIST_TAG)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        Text(stringResource(R.string.settings_theme_label), style = MaterialTheme.typography.titleMedium)
        ThemeModeSelector(selected = state.themeMode, onSelect = onThemeModeChange)
        Text(stringResource(R.string.settings_language_label), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            AppLanguage.entries.forEach { language ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.language == language,
                            onClick = { onLanguageChange(language) },
                            role = Role.RadioButton,
                        ).padding(vertical = Spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppRadioButton(selected = state.language == language, onClick = null)
                    Text(language.nativeName, Modifier.padding(start = Spacing.s3))
                }
            }
        }
        if (BuildConfig.DEBUG) {
            // Debug builds only: lets you try the crash fallback shown on the next launch.
            SectionHeader(stringResource(R.string.settings_developer))
            AppButton(
                stringResource(R.string.settings_crash),
                onClick = { throw IllegalStateException("Test crash from Settings") },
                variant = ButtonVariant.DESTRUCTIVE,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SettingsScreenPreview() {
    PreviewTheme {
        SettingsScreen(
            state = SettingsUiState(themeMode = ThemeMode.SYSTEM, language = AppLanguage.EN),
            onThemeModeChange = {},
            onLanguageChange = {},
            onBack = {},
        )
    }
}
