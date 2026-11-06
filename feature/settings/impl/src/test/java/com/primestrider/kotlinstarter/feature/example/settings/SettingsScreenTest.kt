package com.primestrider.kotlinstarter.feature.example.settings

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.core.i18n.AppLanguage
import com.primestrider.kotlinstarter.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SettingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var theme: ThemeMode? = null
    private var language: AppLanguage? = null

    private fun show() =
        rule.setContent {
            AppTheme(darkTheme = false) {
                Surface {
                    SettingsScreen(
                        state = SettingsUiState(ThemeMode.SYSTEM, AppLanguage.EN),
                        onThemeModeChange = { theme = it },
                        onLanguageChange = { language = it },
                        onBack = {},
                    )
                }
            }
        }

    @Test
    fun currentThemeSegmentIsSelected() {
        show()
        rule.onNodeWithText("System").assertIsSelected()
    }

    @Test
    fun clickingDarkReportsDark() {
        show()
        rule.onNodeWithText("Dark").performClick()
        assertEquals(ThemeMode.DARK, theme)
    }

    @Test
    fun clickingIndonesianReportsId() {
        show()
        rule.onNodeWithText("Bahasa Indonesia").performClick()
        assertEquals(AppLanguage.ID, language)
    }

    @Test
    fun developerCrashButtonShownInDebugBuilds() {
        show()
        rule.onNodeWithTag(SETTINGS_LIST_TAG).performScrollToNode(hasText("Crash the app"))
        rule.onNodeWithText("Crash the app").assertIsDisplayed()
    }
}
