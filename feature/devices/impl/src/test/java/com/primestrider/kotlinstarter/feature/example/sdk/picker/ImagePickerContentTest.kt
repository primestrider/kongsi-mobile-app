package com.primestrider.kotlinstarter.feature.example.sdk.picker

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ImagePickerContentTest {
    @get:Rule
    val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(
        state: PickerState = PickerState(),
        options: PickerOptions = PickerOptions(),
        cameraBlocked: Boolean = false,
    ) = rule.setContent {
        var current by remember { mutableStateOf(options) }
        AppTheme(false) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ImagePickerContent(
                    state = state,
                    options = current,
                    documentType = DocumentType.ANY,
                    onSource = { current = current.copy(source = it) },
                    onCrop = { current = current.copy(crop = it) },
                    onMultiple = { current = current.copy(multiple = it) },
                    onQuality = { current = current.copy(quality = it) },
                    onDocumentType = {},
                    onLibrary = { calls += "library" },
                    onCamera = { calls += "camera" },
                    onChooseFiles = { calls += "files" },
                    cameraBlocked = cameraBlocked,
                    onOpenSettings = { calls += "settings" },
                )
            }
        }
    }

    @Test
    fun empty_state_shows_only_the_controls() {
        show()
        rule.onNodeWithText("Library").assertExists()
        rule.onNodeWithText("Camera").assertExists()
        rule.onNodeWithText("80%").assertExists()
        rule.onNodeWithText("MIME").assertDoesNotExist()
        rule.onNodeWithText("Camera permission was denied.").assertDoesNotExist()
    }

    @Test
    fun a_selection_shows_type_size_file_and_mime_of_the_first_item() {
        val media = PickedMedia(Uri.parse("file:///none.jpg"), "image/jpeg", 4000, 3000, 1536)
        show(PickerState(media = listOf(media)))
        rule.onNodeWithText("Type").assertExists()
        rule.onNodeWithText("4000 × 3000").assertExists()
        rule.onNodeWithText("1.5 KB").assertExists()
        rule.onNodeWithText("image/jpeg").assertExists()
    }

    @Test
    fun picked_documents_list_name_and_size() {
        show(PickerState(documents = listOf(PickedDocument("report.pdf", 2048))))
        rule.onNodeWithText("report.pdf").assertExists()
        rule.onNodeWithText("2.0 KB").assertExists()
    }

    @Test
    fun an_error_is_shown_as_a_banner() {
        show(PickerState(error = PickerError.CAMERA_DENIED))
        rule.onNodeWithText("Camera permission was denied.").assertExists()
    }

    @Test
    fun a_blocked_camera_permission_offers_the_settings_page() {
        show(PickerState(error = PickerError.CAMERA_DENIED), cameraBlocked = true)
        rule.onNodeWithText("Camera permission was denied.").assertExists()
        rule.onNodeWithText("Enable it in Settings to continue.", substring = true).assertExists()
        rule.onNodeWithText("Open Settings").performScrollTo().performClick()
        assertEquals(listOf("settings"), calls)
    }

    @Test
    fun a_plain_denial_has_no_settings_button() {
        show(PickerState(error = PickerError.CAMERA_DENIED))
        rule.onNodeWithText("Open Settings").assertDoesNotExist()
    }

    @Test
    fun crop_is_disabled_while_multiple_selection_is_on() {
        show(options = PickerOptions(multiple = true))
        rule.onNodeWithText("Crop (square)").assertIsNotEnabled()
    }

    @Test
    fun buttons_report_their_action() {
        show()
        rule.onNodeWithText("Library").performScrollTo().performClick()
        rule.onNodeWithText("Camera").performScrollTo().performClick()
        rule.onNodeWithText("Choose files").performScrollTo().performClick()
        assertEquals(listOf("library", "camera", "files"), calls)
        rule.onNodeWithText("Choose files").assertIsEnabled()
    }
}
