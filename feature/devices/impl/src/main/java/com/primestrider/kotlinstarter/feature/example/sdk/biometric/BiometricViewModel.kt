package com.primestrider.kotlinstarter.feature.example.sdk.biometric

import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.feature.devices.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Prefixed: SdkDemoRoute's `id` and `amount` arguments land in this same SavedStateHandle.
private const val FALLBACK_KEY = "biometric_fallback"

enum class BiometricStrength { STRONG, WEAK }

/** The "Device support" rows. */
data class BiometricSupport(
    val hardware: Boolean,
    val enrolled: Boolean,
    val strength: BiometricStrength?,
    val secureStorage: Boolean,
) {
    /** "Verify it's me" needs hardware with something enrolled. */
    val canVerify: Boolean get() = hardware && enrolled
}

/** Maps `BiometricManager.canAuthenticate` results for BIOMETRIC_WEAK and BIOMETRIC_STRONG to [BiometricSupport]. */
fun biometricSupport(
    weak: Int,
    strong: Int,
    secureStorage: Boolean,
): BiometricSupport {
    val noHardware =
        weak == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
            weak == BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ||
            weak == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
    val strength =
        when {
            strong == BiometricManager.BIOMETRIC_SUCCESS -> BiometricStrength.STRONG
            weak == BiometricManager.BIOMETRIC_SUCCESS -> BiometricStrength.WEAK
            else -> null
        }
    return BiometricSupport(
        hardware = !noHardware,
        enrolled = weak == BiometricManager.BIOMETRIC_SUCCESS,
        strength = strength,
        secureStorage = secureStorage,
    )
}

/**
 * A status banner: a short one-line [title] (formatted with [titleArg] when there is one) and, for anything longer, the
 * full sentence as the banner message: [detail] from resources, or [detailText] from the system (a prompt's errString).
 */
data class BiometricStatus(
    @StringRes val title: Int,
    val variant: BannerVariant,
    val titleArg: String? = null,
    @StringRes val detail: Int? = null,
    val detailText: String? = null,
    /** True for the "Stored PIN" status, which shows the PIN itself. */
    val revealsPin: Boolean = false,
)

data class BiometricUiState(
    val allowFallback: Boolean = true,
    val status: BiometricStatus? = null,
)

/**
 * Validation and status messages only; the Keystore and the prompt stay in the screen. The PIN never enters this
 * ViewModel's [SavedStateHandle]: only the fallback switch is saved, and the status (which can show the revealed PIN)
 * lives in memory.
 */
@HiltViewModel
class BiometricViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val status = MutableStateFlow<BiometricStatus?>(null)

        val state: StateFlow<BiometricUiState> =
            combine(savedStateHandle.getStateFlow(FALLBACK_KEY, true), status, ::BiometricUiState)
                .stateIn(viewModelScope, SharingStarted.Eagerly, BiometricUiState(allowFallback = savedStateHandle[FALLBACK_KEY] ?: true))

        fun setAllowFallback(allow: Boolean) {
            savedStateHandle[FALLBACK_KEY] = allow
        }

        /** True when [pin] is valid and the screen should prompt to save it; otherwise shows the validation error. */
        fun onSave(pin: String): Boolean {
            if (isValidPin(pin)) return true
            show(
                BiometricStatus(
                    R.string.sdk_biometric_status_invalid_pin,
                    BannerVariant.ERROR,
                    detail = R.string.sdk_biometric_status_invalid_pin_detail,
                ),
            )
            return false
        }

        fun onVerified() = show(BiometricStatus(R.string.sdk_biometric_status_verified, BannerVariant.SUCCESS))

        fun onSaved() =
            show(
                BiometricStatus(
                    R.string.sdk_biometric_status_saved,
                    BannerVariant.SUCCESS,
                    detail = R.string.sdk_biometric_status_saved_detail,
                ),
            )

        fun onRevealed(pin: String) =
            show(BiometricStatus(R.string.sdk_biometric_status_stored, BannerVariant.INFO, titleArg = pin, revealsPin = true))

        fun onDeleted() = show(BiometricStatus(R.string.sdk_biometric_status_removed, BannerVariant.INFO))

        /** The prompt ended without success (cancel, lockout, nothing enrolled...); [message] is the system's text for it. */
        fun onPromptError(message: String) =
            show(BiometricStatus(R.string.sdk_biometric_status_not_verified, BannerVariant.ERROR, detailText = message.ifBlank { null }))

        /** The key was invalidated by a biometric change; the screen already deleted the PIN. */
        fun onKeyInvalidated() =
            show(
                BiometricStatus(
                    R.string.sdk_biometric_status_invalidated,
                    BannerVariant.WARNING,
                    detail = R.string.sdk_biometric_status_invalidated_detail,
                ),
            )

        /** The Keystore refused (no secure lock screen, nothing enrolled, no Keystore). */
        fun onStorageFailed() =
            show(
                BiometricStatus(
                    R.string.sdk_biometric_status_storage_failed,
                    BannerVariant.ERROR,
                    detail = R.string.sdk_biometric_status_storage_failed_detail,
                ),
            )

        /**
         * The screen went to the background: a revealed PIN is cleared so it is not left on screen (or in a Recents
         * snapshot) when the user comes back. Other statuses stay.
         */
        fun onStopped() {
            if (status.value?.revealsPin == true) status.value = null
        }

        fun dismissStatus() {
            status.value = null
        }

        private fun show(value: BiometricStatus) {
            status.value = value
        }
    }
