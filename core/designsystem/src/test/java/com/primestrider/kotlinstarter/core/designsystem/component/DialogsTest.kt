package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DialogsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var confirms = 0
    private var dismisses = 0

    private fun show(
        dismissible: Boolean = true,
        dismissLabel: String? = "Cancel",
    ) {
        rule.setContent {
            AppTheme(false) {
                ConfirmDialog(
                    title = "Delete?",
                    message = "This cannot be undone.",
                    confirmLabel = "Delete",
                    onConfirm = { confirms++ },
                    onDismiss = { dismisses++ },
                    dismissLabel = dismissLabel,
                    destructive = true,
                    dismissible = dismissible,
                )
            }
        }
        rule.waitForIdle()
    }

    private fun pressBack() {
        rule.runOnUiThread { (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun confirm_and_dismiss_buttons_report() {
        show()
        rule.onNodeWithText("Delete").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(1, confirms)
        assertEquals(1, dismisses)
    }

    @Test
    fun dismissible_dialog_reports_back() {
        show()
        pressBack()
        assertEquals(1, dismisses)
    }

    @Test
    fun non_dismissible_dialog_ignores_back() {
        show(dismissible = false)
        pressBack()
        assertEquals(0, dismisses)
        rule.onNode(isDialog()).assertExists()
    }

    @Test
    fun no_dismiss_button_when_label_is_null() {
        show(dismissLabel = null)
        rule.onNodeWithText("Cancel").assertDoesNotExist()
    }
}
