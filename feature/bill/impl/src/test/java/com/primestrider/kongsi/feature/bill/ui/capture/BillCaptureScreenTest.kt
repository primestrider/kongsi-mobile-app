package com.primestrider.kongsi.feature.bill.ui.capture

import android.content.Intent
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.component.LocalMessenger
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w400dp-h800dp")
class BillCaptureScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()
    private val messenger = Messenger()

    private fun show(status: ReadStatus = ReadStatus.Idle) =
        rule.setContent {
            AppTheme(false) {
                CompositionLocalProvider(LocalMessenger provides messenger) {
                    BillCaptureScreen(
                        status = status,
                        onBack = { events += "back" },
                        onPhoto = { events += "photo $it" },
                        onManual = { events += "manual" },
                        onDismissFailure = { events += "dismiss" },
                    )
                }
            }
        }

    private fun lastLaunched(): Intent = shadowOf(rule.activity).nextStartedActivityForResult.intent

    @Test
    fun taking_a_photo_opens_the_camera_in_the_app_which_first_asks_for_access() {
        show()

        rule.onNodeWithText("Take a photo").performClick()

        rule.onNodeWithText("Kongsi needs the camera", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Choose from gallery").assertDoesNotExist()
        assertNull(shadowOf(rule.activity).nextStartedActivityForResult)
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun the_arrow_on_the_camera_returns_to_the_choices() {
        show()
        rule.onNodeWithText("Take a photo").performClick()

        rule.onNodeWithContentDescription("Back").performClick()

        rule.onNodeWithText("Choose from gallery").assertIsDisplayed()
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun system_back_on_the_camera_returns_to_the_choices() {
        show()
        rule.onNodeWithText("Take a photo").performClick()

        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }

        rule.onNodeWithText("Choose from gallery").assertIsDisplayed()
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun choosing_from_the_gallery_opens_the_photo_picker_for_images() {
        show()

        rule.onNodeWithText("Choose from gallery").performClick()

        assertEquals(MediaStore.ACTION_PICK_IMAGES, lastLaunched().action)
    }

    @Test
    fun while_reading_the_screen_says_so_instead_of_the_choices() {
        show(ReadStatus.Reading)

        rule.onNodeWithText("Reading the receipt…").assertIsDisplayed()
        rule.onNodeWithText("Take a photo").assertDoesNotExist()
    }

    @Test
    fun a_failed_read_says_why_above_the_choices() {
        show(ReadStatus.Failed(ReadFailure.NOTHING_FOUND))

        rule.onNodeWithText("No items found on the photo").assertIsDisplayed()
        rule.onNodeWithText("Try a sharper photo in good light, or type the items in.").assertIsDisplayed()
        rule.onNodeWithText("Type it in myself").assertIsDisplayed()
    }

    @Test
    fun entering_it_by_hand_skips_the_photo() {
        show()

        rule.onNodeWithText("Type it in myself").performClick()

        assertEquals(listOf("manual"), events)
    }
}
