package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PointerScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show() = rule.setContent { AppTheme(false) { PointerScreen(onBack = {}) } }

    @Test
    fun pointer_visualiser_counts_pointers() {
        show()
        rule.onNodeWithText("Pointers: 0").assertExists()
        val area = rule.onNodeWithTag("pointerArea").performScrollTo()
        area.performTouchInput { down(center) }
        rule.onNodeWithText("Pointers: 1").assertExists()
        area.performTouchInput { up() }
        rule.onNodeWithText("Pointers: 0").assertExists()
    }

    @Test
    fun drawing_adds_a_stroke_and_clear_removes_it() {
        show()
        rule.onNodeWithText("Strokes: 0").assertExists()
        rule.onNodeWithTag("canvas").performScrollTo().performTouchInput { swipeUp() }
        rule.onNodeWithText("Strokes: 1").assertExists()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Strokes: 0").assertExists()
    }

    @Test
    fun child_consumes_before_parent() {
        show()
        rule.onNodeWithText("Child: 0 · Parent: 0").assertExists()
        rule.onNodeWithTag("childTarget").performScrollTo().performClick()
        rule.onNodeWithText("Child: 1 · Parent: 0").assertExists()
        rule.onNodeWithText("Child consumes").performScrollTo().performClick()
        rule.onNodeWithTag("childTarget").performScrollTo().performClick()
        rule.onNodeWithText("Child: 2 · Parent: 1").assertExists()
    }

    @Test
    fun strokes_survive_recreation() {
        val restore = StateRestorationTester(rule)
        restore.setContent { AppTheme(false) { PointerScreen(onBack = {}) } }
        rule.onNodeWithTag("canvas").performScrollTo().performTouchInput { swipeUp() }
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Strokes: 1").assertExists()
    }
}
