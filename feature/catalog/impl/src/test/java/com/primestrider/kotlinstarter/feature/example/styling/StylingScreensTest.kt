package com.primestrider.kotlinstarter.feature.example.styling

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.core.model.ThemeMode
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import com.primestrider.kotlinstarter.core.designsystem.R as DesignR

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class StylingScreensTest(
    private val page: Page,
) {
    enum class Page(
        @StringRes val title: Int,
        val content: @Composable (onBack: () -> Unit) -> Unit,
    ) {
        COLORS(R.string.example_styling_colors_title, { ColorsScreen(it) }),
        TYPOGRAPHY(R.string.example_styling_typography_title, { TypographyScreen(it) }),
        SPACING(R.string.example_styling_spacing_title, { SpacingScreen(it) }),
        SIZING(R.string.example_styling_sizing_title, { SizingScreen(it) }),
        LAYOUT(R.string.example_styling_layout_title, { LayoutScreen(it) }),
        APPEARANCE(R.string.example_styling_appearance_title, { AppearanceScreen(it) }),
        THEME(R.string.example_styling_theme_title, { ThemeScreen(ThemeMode.SYSTEM, {}, it) }),
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun pages(): List<Page> = Page.entries
    }

    @get:Rule
    val rule = createComposeRule()

    private fun string(res: Int) = ApplicationProvider.getApplicationContext<android.content.Context>().getString(res)

    @Test
    fun showsTitle() {
        rule.setContent { AppTheme(darkTheme = false) { page.content {} } }
        rule.onNodeWithText(string(page.title)).assertIsDisplayed()
    }

    @Test
    fun backButtonInvokesOnBack() {
        var backs = 0
        rule.setContent { AppTheme(darkTheme = false) { page.content { backs++ } } }
        rule.onNodeWithContentDescription(string(DesignR.string.component_back)).performClick()
        assertEquals(1, backs)
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class StylingNewSectionsTest(
    private val section: Section,
) {
    enum class Section(
        @StringRes val label: Int,
        val content: @Composable () -> Unit,
    ) {
        TEXT_COLORS(R.string.styling_section_text_colors, { ColorsScreen {} }),
        TEXT_ALIGNMENT(R.string.styling_section_text_alignment, { TypographyScreen {} }),
        TEXT_DECORATION(R.string.styling_section_text_decoration, { TypographyScreen {} }),
        MARGIN(R.string.styling_section_margin, { SpacingScreen {} }),
        NEGATIVE_MARGIN(R.string.styling_section_negative_margin, { SpacingScreen {} }),
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun sections(): List<Section> = Section.entries
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun sectionLabelIsDisplayed() {
        rule.setContent { AppTheme(darkTheme = false) { section.content() } }
        val label = ApplicationProvider.getApplicationContext<android.content.Context>().getString(section.label)
        rule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
    }
}
