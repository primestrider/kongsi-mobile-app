package com.primestrider.kongsi.feature.bill.ui.capture

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.component.LocalMessenger
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w400dp-h800dp")
class BillCaptureScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()
    private val messenger = Messenger()

    private fun show() =
        rule.setContent {
            AppTheme(false) {
                CompositionLocalProvider(LocalMessenger provides messenger) {
                    BillCaptureScreen(
                        onBack = { events += "back" },
                        onPhoto = { events += "photo $it" },
                        onManual = { events += "manual" },
                    )
                }
            }
        }

    private fun lastLaunched(): Intent = shadowOf(rule.activity).nextStartedActivityForResult.intent

    @Test
    fun taking_a_photo_opens_the_camera_app_writing_to_our_own_file() {
        show()

        rule.onNodeWithText("Take a photo").performClick()

        val intent = lastLaunched()
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, intent.action)
        @Suppress("DEPRECATION")
        val output = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
        assertEquals("${rule.activity.packageName}.receipts", output.authority)
    }

    @Test
    fun a_photo_taken_is_handed_on() {
        show()
        rule.onNodeWithText("Take a photo").performClick()
        val intent = lastLaunched()

        @Suppress("DEPRECATION")
        val output = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!

        shadowOf(rule.activity).receiveResult(intent, Activity.RESULT_OK, Intent())
        rule.waitForIdle()

        assertEquals(listOf("photo $output"), events)
    }

    @Test
    fun a_cancelled_photo_is_deleted_and_nothing_is_handed_on() {
        show()
        rule.onNodeWithText("Take a photo").performClick()
        val intent = lastLaunched()

        @Suppress("DEPRECATION")
        val output = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
        val file = File(File(rule.activity.cacheDir, "receipts"), output.lastPathSegment!!)
        assertTrue(file.exists())

        shadowOf(rule.activity).receiveResult(intent, Activity.RESULT_CANCELED, null)
        rule.waitForIdle()

        assertFalse(file.exists())
        assertTrue(events.isEmpty())
    }

    @Test
    fun choosing_from_the_gallery_opens_the_photo_picker_for_images() {
        show()

        rule.onNodeWithText("Choose from gallery").performClick()

        assertEquals(MediaStore.ACTION_PICK_IMAGES, lastLaunched().action)
    }

    @Test
    fun entering_it_by_hand_skips_the_photo() {
        show()

        rule.onNodeWithText("Type it in myself").performClick()

        assertEquals(listOf("manual"), events)
    }
}
