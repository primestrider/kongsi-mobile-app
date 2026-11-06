package com.primestrider.kotlinstarter.feature.example.sdk.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BiometricViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val created = mutableListOf<ViewModel>()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = BiometricViewModel(handle).also { created += it }

    // (title, detail) as the banner shows them.
    private fun BiometricStatus?.shown(): Pair<String, String?>? =
        this?.let {
            context.getString(it.title, *listOfNotNull(it.titleArg).toTypedArray()) to (it.detailText ?: it.detail?.let(context::getString))
        }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun an_invalid_pin_is_rejected_without_a_prompt() {
        val vm = viewModel()
        assertFalse(vm.onSave("12a456"))
        val status = vm.state.value.status
        assertEquals("Invalid PIN" to "PIN must be exactly 6 digits.", status.shown())
        assertEquals(BannerVariant.ERROR, status?.variant)
    }

    @Test
    fun a_valid_pin_asks_for_the_prompt() {
        val vm = viewModel()
        assertTrue(vm.onSave("123456"))
        assertNull(vm.state.value.status)
    }

    @Test
    fun the_saved_state_never_holds_the_pin() {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.onSave("123456")
        vm.onSaved()
        vm.onRevealed("123456")
        vm.setAllowFallback(false)
        assertEquals(
            "Stored PIN: 123456" to null,
            vm.state.value.status
                .shown(),
        )
        handle.keys().forEach { key ->
            assertFalse("$key holds the PIN", handle.get<Any?>(key).toString().contains("123456"))
            assertFalse("$key is a route argument", key == "id" || key == "amount")
        }
    }

    @Test
    fun the_fallback_switch_survives_a_new_view_model() {
        val handle = SavedStateHandle()
        assertTrue(viewModel(handle).state.value.allowFallback)
        viewModel(handle).setAllowFallback(false)
        assertFalse(viewModel(handle).state.value.allowFallback)
    }

    @Test
    fun outcomes_map_to_a_short_title_and_a_full_detail() {
        val vm = viewModel()
        vm.onVerified()
        assertEquals(
            "Verified." to null,
            vm.state.value.status
                .shown(),
        )
        assertEquals(
            BannerVariant.SUCCESS,
            vm.state.value.status
                ?.variant,
        )
        vm.onSaved()
        assertEquals(
            "PIN saved" to "PIN saved to secure storage.",
            vm.state.value.status
                .shown(),
        )
        vm.onDeleted()
        assertEquals(
            "PIN removed." to null,
            vm.state.value.status
                .shown(),
        )
        vm.onPromptError("Too many attempts. Use screen lock instead.")
        assertEquals(
            "Not verified" to "Too many attempts. Use screen lock instead.",
            vm.state.value.status
                .shown(),
        )
        assertEquals(
            BannerVariant.ERROR,
            vm.state.value.status
                ?.variant,
        )
        vm.onKeyInvalidated()
        assertEquals(
            "Biometrics changed" to "The PIN was removed.",
            vm.state.value.status
                .shown(),
        )
        vm.onStorageFailed()
        assertEquals(
            "Secure storage failed" to "Enroll a fingerprint or face, then try again.",
            vm.state.value.status
                .shown(),
        )
        assertEquals(
            BannerVariant.ERROR,
            vm.state.value.status
                ?.variant,
        )
        vm.dismissStatus()
        assertNull(vm.state.value.status)
    }

    @Test
    fun support_rows_follow_the_authenticator_results() {
        val strong = biometricSupport(BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_SUCCESS, secureStorage = true)
        assertEquals(BiometricSupport(hardware = true, enrolled = true, strength = BiometricStrength.STRONG, secureStorage = true), strong)

        val weakOnly =
            biometricSupport(BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED, secureStorage = true)
        assertEquals(BiometricStrength.WEAK, weakOnly.strength)

        val notEnrolled =
            biometricSupport(
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
                secureStorage = true,
            )
        assertEquals(BiometricSupport(hardware = true, enrolled = false, strength = null, secureStorage = true), notEnrolled)

        val none =
            biometricSupport(
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                secureStorage = false,
            )
        assertEquals(BiometricSupport(hardware = false, enrolled = false, strength = null, secureStorage = false), none)
        assertFalse(none.canVerify)
        assertTrue(strong.canVerify)
    }

    @Test
    fun going_to_the_background_clears_a_revealed_pin() {
        val vm = viewModel()
        vm.onRevealed("123456")
        assertTrue(
            vm.state.value.status
                ?.revealsPin == true,
        )
        vm.onStopped()
        assertNull(vm.state.value.status)
    }

    @Test
    fun going_to_the_background_keeps_other_statuses() {
        val vm = viewModel()
        vm.onVerified()
        vm.onStopped()
        assertEquals(
            BannerVariant.SUCCESS,
            vm.state.value.status
                ?.variant,
        )
    }
}
