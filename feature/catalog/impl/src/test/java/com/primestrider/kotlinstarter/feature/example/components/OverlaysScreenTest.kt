package com.primestrider.kotlinstarter.feature.example.components

import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class OverlaysScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        rule.setContent { AppTheme(darkTheme = false) { OverlaysScreen(onBack = {}) } }
    }

    @Test
    fun blocking_dialog_ignores_back() {
        rule.onNodeWithText("Open blocking dialog").performScrollTo().performClick()
        rule.waitForIdle()
        rule.runOnUiThread { (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed() }
        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("Accept").performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun cancel_closes_dialog() {
        rule.onNodeWithText("Open dialog").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun bottom_sheet_choice_is_shown_on_page() {
        rule.onNodeWithText("Open bottom sheet").performScrollTo().performClick()
        rule.onNodeWithText("Oldest").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Sorted by: Oldest").assertExists()
    }
}
