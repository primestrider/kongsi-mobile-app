package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
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
class ScrollScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show() = rule.setContent { AppTheme(false) { ScrollScreen(onBack = {}) } }

    @Test
    fun dial_scrolls_its_value() {
        show()
        rule.onNodeWithText("Value: 0").assertExists()
        rule.onNodeWithTag("dial").performScrollTo().performTouchInput { swipeUp() }
        rule.onNodeWithText("Value: 0").assertDoesNotExist()
    }

    @Test
    fun dial_buttons_are_the_accessible_alternative() {
        show()
        rule.onNodeWithContentDescription("Increase").performScrollTo().performClick()
        rule.onNodeWithText("Value: 5").assertExists()
    }

    @Test
    fun nested_header_collapses_before_list_scrolls() {
        show()
        rule.onNodeWithTag("nestedList").performScrollTo()
        val before =
            rule
                .onNodeWithTag("nestedHeader")
                .fetchSemanticsNode()
                .size.height
        rule.onNodeWithTag("nestedList").performTouchInput { swipeUp() }
        rule.waitForIdle()
        val after =
            rule
                .onNodeWithTag("nestedHeader")
                .fetchSemanticsNode()
                .size.height
        assertTrue("header shrank: $before -> $after", after < before)
    }

    @Test
    fun overscroll_lists_render() {
        show()
        rule.onNodeWithTag("overscrollList").performScrollTo().performScrollToNode(hasText("Row 1"))
        rule.onNodeWithTag("noOverscrollList").performScrollTo().performScrollToNode(hasText("Row 20"))
    }
}
