package com.primestrider.kotlinstarter.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AppThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(dark: Boolean): Pair<Color, Color> {
        var result: Pair<Color, Color>? = null
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                result = MaterialTheme.colorScheme.background to AppThemeTokens.colors.success
            }
        }
        composeRule.waitForIdle()
        return checkNotNull(result)
    }

    private fun captureScheme(dark: Boolean): ColorScheme {
        var result: ColorScheme? = null
        composeRule.setContent {
            AppTheme(darkTheme = dark) { result = MaterialTheme.colorScheme }
        }
        composeRule.waitForIdle()
        return checkNotNull(result)
    }

    private fun assertNoBaselineRole(
        scheme: ColorScheme,
        baseline: ColorScheme,
    ) {
        val roles: List<Pair<String, (ColorScheme) -> Color>> =
            listOf(
                "primaryContainer" to { it.primaryContainer },
                "secondary" to { it.secondary },
                "tertiary" to { it.tertiary },
                "tertiaryContainer" to { it.tertiaryContainer },
                "errorContainer" to { it.errorContainer },
                "surfaceContainer" to { it.surfaceContainer },
                "surfaceContainerHigh" to { it.surfaceContainerHigh },
                "inverseSurface" to { it.inverseSurface },
            )
        val unmapped = roles.filter { (_, role) -> role(scheme) == role(baseline) }.map { it.first }
        assertTrue("Roles still at the M3 baseline: $unmapped", unmapped.isEmpty())
    }

    /**
     * Every `ColorScheme` role, found by reflection so a role added by a Material 3 upgrade is checked too.
     * Compose `Color` is an inline class, so each getter returns a raw `long`.
     */
    private fun allRoles(scheme: ColorScheme): Map<String, Color> =
        ColorScheme::class.java.methods
            .filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == java.lang.Long.TYPE }
            .associate { it.name.removePrefix("get").substringBefore('-') to Color((it.invoke(scheme) as Long).toULong()) }

    private val tokenColors: Set<Color> =
        Palette.scales.flatMap { (_, steps) -> steps.map { it.second } }.toSet() + setOf(Palette.white, Palette.black)

    private fun assertEveryRoleIsAToken(dark: Boolean) {
        val roles = allRoles(captureScheme(dark))
        assertTrue("Found too few roles by reflection: ${roles.keys}", roles.size >= 40)
        val offPalette = roles.filterValues { it !in tokenColors }.keys
        assertTrue("Roles not mapped to a palette token (dark=$dark): $offPalette", offPalette.isEmpty())
    }

    @Test
    fun every_role_is_a_token_in_light() = assertEveryRoleIsAToken(dark = false)

    @Test
    fun every_role_is_a_token_in_dark() = assertEveryRoleIsAToken(dark = true)

    @Test
    fun all_roles_are_mapped_in_light() {
        val scheme = captureScheme(dark = false)
        assertNoBaselineRole(scheme, lightColorScheme())
        assertEquals(LightColors.info, scheme.tertiary)
        assertEquals(Palette.Gray.c200, scheme.surfaceContainerHigh)
    }

    @Test
    fun all_roles_are_mapped_in_dark() {
        val scheme = captureScheme(dark = true)
        assertNoBaselineRole(scheme, darkColorScheme())
        assertEquals(Palette.Red.c950, scheme.errorContainer)
    }

    @Test
    fun dark_theme_provides_dark_colors() {
        val (background, success) = capture(dark = true)
        assertEquals(DarkColors.background, background)
        assertEquals(DarkColors.success, success)
    }

    @Test
    fun light_theme_provides_light_colors() {
        val (background, success) = capture(dark = false)
        assertEquals(LightColors.background, background)
        assertEquals(LightColors.success, success)
    }
}
