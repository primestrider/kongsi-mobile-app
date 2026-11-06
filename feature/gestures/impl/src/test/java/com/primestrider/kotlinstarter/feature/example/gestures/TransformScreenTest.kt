package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TransformScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show() = rule.setContent { AppTheme(false) { TransformScreen(onBack = {}) } }

    @Test
    fun double_tap_zooms_and_reset_restores() {
        show()
        rule.onNodeWithText("Zoom: 1.0×").assertExists()
        rule.onNodeWithTag("transformSurface").performScrollTo().performTouchInput { doubleClick() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Zoom: 2.5×").assertExists()
        rule.onNodeWithText("Reset").performScrollTo().performClick()
        rule.onNodeWithText("Zoom: 1.0×").assertExists()
    }

    @Test
    fun zoom_buttons_are_the_accessible_alternative() {
        show()
        rule.onNodeWithContentDescription("Zoom in").performScrollTo().performClick()
        rule.onNodeWithText("Zoom: 1.0×").assertDoesNotExist()
    }

    @Test
    fun pinch_zooms() {
        show()
        rule.onNodeWithTag("transformSurface").performScrollTo().performTouchInput {
            pinch(center - Offset(20f, 0f), center - Offset(200f, 0f), center + Offset(20f, 0f), center + Offset(200f, 0f))
        }
        rule.onNodeWithText("Zoom: 1.0×").assertDoesNotExist()
    }

    @Test
    fun a_vertical_drag_on_the_surface_does_not_scroll_the_page_away() {
        show()
        rule.onNodeWithContentDescription("Zoom in").performScrollTo().performClick()
        val surface = rule.onNodeWithTag("transformSurface").performScrollTo()
        val before = surface.fetchSemanticsNode().positionInRoot.y
        surface.performTouchInput { swipeUp() }
        rule.waitForIdle()
        val after =
            rule
                .onNodeWithTag("transformSurface")
                .fetchSemanticsNode()
                .positionInRoot.y
        assertEquals(before, after, 0.5f)
    }

    @Test
    fun zoom_survives_recreation() {
        val restore = StateRestorationTester(rule)
        restore.setContent { AppTheme(false) { TransformScreen(onBack = {}) } }
        rule.onNodeWithContentDescription("Zoom in").performScrollTo().performClick()
        rule.onNodeWithText("Zoom: 1.5×").assertExists()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Zoom: 1.5×").assertExists()
    }
}
