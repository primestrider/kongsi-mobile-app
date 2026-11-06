package com.primestrider.kotlinstarter.feature.example.styling

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.primestrider.kotlinstarter.core.designsystem.R as DesignR

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ThemeScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun themeScreenReportsDark() {
        var mode: ThemeMode? = null
        rule.setContent { AppTheme(darkTheme = false) { ThemeScreen(ThemeMode.SYSTEM, { mode = it }, {}) } }
        val dark = ApplicationProvider.getApplicationContext<android.content.Context>().getString(DesignR.string.component_theme_dark)
        rule.onNodeWithText(dark).performClick()
        assertEquals(ThemeMode.DARK, mode)
    }
}
