package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SkeletonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun skeleton_text_is_announced_as_loading() {
        rule.mainClock.autoAdvance = false
        rule.setContent { AppTheme(false) { SkeletonText(lines = 3) } }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading").assertExists()
    }
}
