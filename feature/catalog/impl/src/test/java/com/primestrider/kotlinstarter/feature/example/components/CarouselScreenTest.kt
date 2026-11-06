package com.primestrider.kotlinstarter.feature.example.components

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class CarouselScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        rule.setContent { AppTheme(false) { CarouselScreen(onBack = {}) } }
    }

    @Test
    fun carousels_render() {
        rule.onNodeWithTag("multiBrowseCarousel").performScrollTo().assertExists()
        rule.onNodeWithTag("uncontainedCarousel").performScrollTo().assertExists()
        rule.onNodeWithTag("heroCarousel").performScrollTo().assertExists()
    }

    @Test
    fun multi_browse_swipe_reveals_later_items() {
        rule.onNodeWithText("Item 8").assertDoesNotExist()
        val carousel = rule.onNodeWithTag("multiBrowseCarousel").performScrollTo()
        repeat(3) { carousel.performTouchInput { swipeLeft() } }
        rule.waitForIdle()
        rule.onNodeWithText("Item 8").assertExists()
    }

    @Test
    fun grids_render() {
        rule.onNodeWithTag("staggeredGrid").performScrollTo().performScrollToNode(hasText("Tile 1"))
        rule.onNodeWithTag("horizontalGrid").performScrollTo().performScrollToNode(hasText("Cell 1"))
    }
}
