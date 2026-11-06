package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DragScreenTest {
    @get:Rule
    val rule = createComposeRule()

    /** Unclipped, so a row scrolled out of the viewport still reports where it is. */
    private fun top(text: String) =
        rule
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .positionInRoot.y

    private fun show() = rule.setContent { AppTheme(false) { DragScreen(onBack = {}) } }

    @Test
    fun horizontal_drag_moves_the_handle() {
        show()
        rule.onNodeWithText("Value: 0").assertExists()
        rule.onNodeWithTag("dragHandle").performScrollTo().performTouchInput { swipeRight() }
        rule.onNodeWithText("Value: 0").assertDoesNotExist()
    }

    @Test
    fun free_drag_inside_the_page_scroll_moves_the_box() {
        show()
        rule.onNodeWithText("Position: 0, 0").assertExists()
        rule.onNodeWithTag("freeDragArea").performScrollTo().performTouchInput { swipeUp() }
        rule.onNodeWithText("Position: 0, 0").assertDoesNotExist()
    }

    @Test
    fun anchored_sheet_snaps_to_an_anchor() {
        show()
        rule.onNodeWithText("Anchor: Collapsed").assertExists()
        rule.onNodeWithTag("anchoredSheet").performScrollTo().performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithText("Anchor: Collapsed").assertDoesNotExist()
    }

    @Test
    fun reorder_has_move_buttons() {
        show()
        rule.onNodeWithContentDescription("Move Item 1 down").performScrollTo().performClick()
        val one = top("Item 1")
        val two = top("Item 2")
        assertTrue("Item 2 is above Item 1", two < one)
    }

    @Test
    fun drag_state_survives_recreation() {
        val restore = StateRestorationTester(rule)
        restore.setContent { AppTheme(false) { DragScreen(onBack = {}) } }
        rule.onNodeWithText("Expand").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Move Item 1 down").performScrollTo().performClick()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Anchor: Expanded").assertExists()
        val one = top("Item 1")
        val two = top("Item 2")
        assertTrue("order kept", two < one)
    }

    @Test
    fun long_press_drag_moves_a_row_several_places() {
        show()
        val row = rule.onNodeWithTag("reorderRow-1").performScrollTo()
        val rowHeight =
            row
                .fetchSemanticsNode()
                .size.height
                .toFloat()
        row.performTouchInput {
            down(Offset(width / 4f, height / 2f))
        }
        // Past the long-press timeout, which runs on the main clock.
        rule.mainClock.advanceTimeBy(1_000)
        // One move per frame, like a real finger: the list recomposes between moves.
        repeat(10) {
            row.performTouchInput { moveBy(Offset(0f, rowHeight * 0.25f)) }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        row.performTouchInput { up() }
        rule.waitForIdle()
        assertTrue("Item 1 below Item 3", top("Item 1") > top("Item 3"))
    }
}
