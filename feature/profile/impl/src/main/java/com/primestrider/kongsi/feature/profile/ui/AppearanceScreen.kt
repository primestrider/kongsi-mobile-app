package com.primestrider.kongsi.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.primestrider.kongsi.core.designsystem.component.AppRadioButton
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.SectionHeader
import com.primestrider.kongsi.core.designsystem.component.ThemeModeSelector
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.core.i18n.AppLanguage
import com.primestrider.kongsi.core.model.ThemeMode
import com.primestrider.kongsi.feature.profile.impl.R

/** Theme (system, light, dark) and app language. */
@Composable
fun AppearanceScreen(
    themeMode: ThemeMode,
    language: AppLanguage,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit,
) {
    AppScaffold(title = stringResource(R.string.profile_section_appearance), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            SectionHeader(stringResource(R.string.profile_theme))
            ThemeModeSelector(selected = themeMode, onSelect = onThemeModeChange)
            SectionHeader(stringResource(R.string.profile_language), Modifier.padding(top = Spacing.s3))
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language == option,
                                onClick = { onLanguageChange(option) },
                                role = Role.RadioButton,
                            ).padding(vertical = Spacing.s2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppRadioButton(selected = language == option, onClick = null)
                        Text(option.nativeName, Modifier.padding(start = Spacing.s3))
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AppearanceScreenPreview() {
    PreviewTheme {
        AppearanceScreen(ThemeMode.SYSTEM, AppLanguage.ID, {}, {}, {})
    }
}
