package com.primestrider.kotlinstarter.feature.example.sdk.biometric

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.feature.devices.impl.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BiometricContentTest {
    @get:Rule
    val rule = createComposeRule()

    private val enrolled = BiometricSupport(hardware = true, enrolled = true, strength = BiometricStrength.STRONG, secureStorage = true)
    private var dismissed = 0

    private fun show(
        support: BiometricSupport = enrolled,
        fallbackSupported: Boolean = true,
        hasPin: Boolean = false,
        status: BiometricStatus? = null,
    ) {
        rule.setContent {
            AppTheme(false) {
                // A narrow phone, so a one-line title would have to ellipsise long text.
                Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                    BiometricContent(
                        support = support,
                        fallbackSupported = fallbackSupported,
                        allowFallback = fallbackSupported,
                        pin = "",
                        hasPin = hasPin,
                        status = status,
                        onAllowFallback = {},
                        onVerify = {},
                        onPin = {},
                        onSave = {},
                        onReveal = {},
                        onDelete = {},
                        onDismissStatus = { dismissed++ },
                    )
                }
            }
        }
    }

    @Test
    fun support_rows_and_verify_for_an_enrolled_device() {
        show()
        rule.onNodeWithText("Strong").assertIsDisplayed()
        rule.onNodeWithText("Available").assertIsDisplayed()
        rule.onNodeWithText("Verify it's me").assertIsEnabled()
        rule.onNodeWithText("Passcode fallback with a protected key needs Android 11.").assertDoesNotExist()
    }

    @Test
    fun verify_is_disabled_without_enrolment() {
        show(support = enrolled.copy(enrolled = false, strength = null))
        rule.onNodeWithText("Verify it's me").assertIsNotEnabled()
    }

    @Test
    fun below_android_11_the_fallback_switch_is_off_and_explained() {
        show(fallbackSupported = false)
        rule.onNodeWithText("Allow device passcode fallback").assertIsNotEnabled()
        rule.onNodeWithText("Passcode fallback with a protected key needs Android 11.").assertIsDisplayed()
    }

    @Test
    fun reveal_and_delete_need_a_stored_pin() {
        show()
        rule.onNodeWithText("Reveal").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Delete").assertIsNotEnabled()
    }

    @Test
    fun the_status_banner_shows_and_closes() {
        show(hasPin = true, status = BiometricStatus(R.string.sdk_biometric_status_stored, BannerVariant.INFO, "123456"))
        rule.onNodeWithText("Reveal").assertIsEnabled()
        rule.onNodeWithText("Stored PIN: 123456").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Stored PIN: 123456", substring = true).performClick()
        assertEquals(1, dismissed)
    }

    // Robolectric does not measure text like a device, so these check the structure: the long text is the banner's
    // wrapping message, a node of its own holding the whole sentence, not part of the one-line, ellipsised title.
    @Test
    fun an_error_shows_its_remedy_in_full_as_its_own_node() {
        show(
            status =
                BiometricStatus(
                    R.string.sdk_biometric_status_storage_failed,
                    BannerVariant.ERROR,
                    detail = R.string.sdk_biometric_status_storage_failed_detail,
                ),
        )
        rule.onNodeWithText("Secure storage failed").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Enroll a fingerprint or face, then try again.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun a_long_system_error_is_the_message_not_the_title() {
        val lockout = "Too many attempts. Fingerprint sensor disabled. Use your screen lock to continue."
        show(status = BiometricStatus(R.string.sdk_biometric_status_not_verified, BannerVariant.ERROR, detailText = lockout))
        rule.onNodeWithText("Not verified").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(lockout).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Not verified: ", substring = true).assertDoesNotExist()
    }
}
