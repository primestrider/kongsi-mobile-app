package com.primestrider.kotlinstarter.feature.example.sdk.biometric

import android.content.Context
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.util.findActivity
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import javax.crypto.Cipher

/** Device credential with a crypto-bound key needs Android 11 (`setUserAuthenticationParameters`). */
private val FALLBACK_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

/** Biometric support, a plain "verify" prompt, and a PIN that only an authenticated Keystore key can encrypt and read. */
@Composable
fun BiometricScreen(
    onBack: () -> Unit,
    viewModel: BiometricViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context.findActivity() as? FragmentActivity
    val state by viewModel.state.collectAsStateWithLifecycle()
    val vault = remember { PinVault(context.applicationContext) }
    // The PIN text is deliberately not saveable (no rememberSaveable): it must never reach the saved instance state.
    var pin by remember { mutableStateOf("") }
    var hasPin by remember { mutableStateOf(vault.hasPin()) }
    var support by remember { mutableStateOf(readSupport(context)) }
    LifecycleResumeEffect(Unit) {
        // Enrolment can change in Settings while this screen is in the background.
        support = readSupport(context)
        hasPin = vault.hasPin()
        onPauseOrDispose {}
    }
    // A revealed PIN must not outlive the visit: no screenshots or Recents thumbnail while it shows, and it is
    // cleared when the app goes to the background.
    SecureWindowEffect(secure = state.status?.revealsPin == true)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onStopped() }
    val allowFallback = FALLBACK_SUPPORTED && state.allowFallback
    val cancel = stringResource(R.string.sdk_biometric_cancel)
    val verifyTitle = stringResource(R.string.sdk_biometric_prompt_verify)
    val saveTitle = stringResource(R.string.sdk_biometric_prompt_save)
    val revealTitle = stringResource(R.string.sdk_biometric_prompt_reveal)

    // Runs a Keystore step: an invalidated key drops the PIN, any other failure becomes a status instead of a crash.
    fun guarded(step: () -> Unit) {
        try {
            step()
        } catch (e: KeyPermanentlyInvalidatedException) {
            vault.delete()
            hasPin = false
            viewModel.onKeyInvalidated()
        } catch (e: Exception) {
            viewModel.onStorageFailed()
        }
    }

    fun prompt(
        title: String,
        authenticators: Int,
        crypto: BiometricPrompt.CryptoObject?,
        onSuccess: (Cipher?) -> Unit,
    ) {
        val host = activity ?: return viewModel.onStorageFailed()
        val callback =
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    guarded { onSuccess(result.cryptoObject?.cipher) }
                }

                // Every error code ends here (cancel, negative button, lockout, timeout, nothing enrolled); errString is localised.
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    viewModel.onPromptError(errString.toString())
                }
            }
        try {
            val info =
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(title)
                    .setAllowedAuthenticators(authenticators)
                    .apply { if (authenticators and DEVICE_CREDENTIAL == 0) setNegativeButtonText(cancel) }
                    .build()
            val biometricPrompt = BiometricPrompt(host, ContextCompat.getMainExecutor(host), callback)
            if (crypto != null) biometricPrompt.authenticate(info, crypto) else biometricPrompt.authenticate(info)
        } catch (e: IllegalArgumentException) {
            // An authenticator combination this Android version does not support.
            viewModel.onPromptError(e.message.orEmpty())
        }
    }

    fun keyAuthenticators(cipher: PinCipher) =
        if (cipher.allowsDeviceCredential) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_STRONG

    ExamplePageScaffold(title = R.string.sdk_biometric_title, subtitle = R.string.sdk_biometric_subtitle, onBack = onBack) {
        BiometricContent(
            support = support,
            fallbackSupported = FALLBACK_SUPPORTED,
            allowFallback = allowFallback,
            pin = pin,
            hasPin = hasPin,
            status = state.status,
            onAllowFallback = viewModel::setAllowFallback,
            onVerify = {
                val authenticators = if (allowFallback) BIOMETRIC_WEAK or DEVICE_CREDENTIAL else BIOMETRIC_WEAK
                prompt(verifyTitle, authenticators, crypto = null) { viewModel.onVerified() }
            },
            onPin = { pin = it },
            onSave = {
                if (viewModel.onSave(pin)) {
                    guarded {
                        val cipher = vault.encryptCipher(allowFallback)
                        prompt(saveTitle, keyAuthenticators(cipher), BiometricPrompt.CryptoObject(cipher.cipher)) { authenticated ->
                            vault.save(PinCipher(checkNotNull(authenticated), cipher.allowsDeviceCredential), pin)
                            pin = ""
                            hasPin = true
                            viewModel.onSaved()
                        }
                    }
                }
            },
            onReveal = {
                guarded {
                    val cipher = vault.decryptCipher()
                    if (cipher == null) {
                        hasPin = false
                    } else {
                        prompt(revealTitle, keyAuthenticators(cipher), BiometricPrompt.CryptoObject(cipher.cipher)) { authenticated ->
                            vault.read(checkNotNull(authenticated))?.let(viewModel::onRevealed)
                        }
                    }
                }
            },
            onDelete = {
                vault.delete()
                hasPin = false
                viewModel.onDeleted()
            },
            onDismissStatus = viewModel::dismissStatus,
        )
    }
}

private fun readSupport(context: Context): BiometricSupport {
    val manager = BiometricManager.from(context)
    return biometricSupport(
        weak = manager.canAuthenticate(BIOMETRIC_WEAK),
        strong = manager.canAuthenticate(BIOMETRIC_STRONG),
        secureStorage = PinVault.isKeystoreAvailable(),
    )
}

/** The three sections and the status banner; nothing here touches the Keystore or shows a prompt. */
@Composable
internal fun BiometricContent(
    support: BiometricSupport,
    fallbackSupported: Boolean,
    allowFallback: Boolean,
    pin: String,
    hasPin: Boolean,
    status: BiometricStatus?,
    onAllowFallback: (Boolean) -> Unit,
    onVerify: () -> Unit,
    onPin: (String) -> Unit,
    onSave: () -> Unit,
    onReveal: () -> Unit,
    onDelete: () -> Unit,
    onDismissStatus: () -> Unit,
) {
    val yes = stringResource(R.string.sdk_biometric_yes)
    val no = stringResource(R.string.sdk_biometric_no)
    SdkSection(R.string.sdk_biometric_support_title, R.string.sdk_biometric_support_description) {
        InfoCard(
            listOf(
                stringResource(R.string.sdk_biometric_hardware) to if (support.hardware) yes else no,
                stringResource(R.string.sdk_biometric_enrolled) to if (support.enrolled) yes else no,
                stringResource(R.string.sdk_biometric_strength) to
                    support.strength?.let {
                        stringResource(if (it == BiometricStrength.STRONG) R.string.sdk_biometric_strong else R.string.sdk_biometric_weak)
                    },
                stringResource(R.string.sdk_biometric_storage) to
                    stringResource(if (support.secureStorage) R.string.sdk_biometric_available else R.string.sdk_biometric_unavailable),
            ),
        )
    }
    SdkSection(R.string.sdk_biometric_auth_title, R.string.sdk_biometric_auth_description) {
        SwitchRow(
            label = stringResource(R.string.sdk_biometric_fallback),
            checked = allowFallback,
            onCheckedChange = onAllowFallback,
            enabled = fallbackSupported,
        )
        if (!fallbackSupported) {
            Text(
                stringResource(R.string.sdk_biometric_fallback_needs_api),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppButton(stringResource(R.string.sdk_biometric_verify), onClick = onVerify, enabled = support.canVerify)
    }
    SdkSection(R.string.sdk_biometric_pin_title, R.string.sdk_biometric_pin_description) {
        AppTextField(
            value = pin,
            onValueChange = { value -> onPin(value.filter { it in '0'..'9' }) },
            label = stringResource(R.string.sdk_biometric_pin_label),
            placeholder = stringResource(R.string.sdk_biometric_pin_placeholder),
            keyboardType = KeyboardType.NumberPassword,
            maxLength = 6,
            visualTransformation = PasswordVisualTransformation(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            AppButton(stringResource(R.string.sdk_biometric_save), onClick = onSave)
            AppButton(stringResource(R.string.sdk_biometric_reveal), onClick = onReveal, variant = ButtonVariant.OUTLINED, enabled = hasPin)
            AppButton(stringResource(R.string.sdk_biometric_delete), onClick = onDelete, variant = ButtonVariant.TEXT, enabled = hasPin)
        }
    }
    status?.let {
        Banner(
            // The title is one line; anything longer (a remedy, the system's error text) goes in the wrapping message.
            title = stringResource(it.title, *listOfNotNull(it.titleArg).toTypedArray()),
            message = it.detailText ?: it.detail?.let { detail -> stringResource(detail) },
            variant = it.variant,
            onDismiss = onDismissStatus,
        )
    }
}

/** Adds `FLAG_SECURE` to the window while [secure] is true; only clears the flag if it set it itself. */
@Composable
private fun SecureWindowEffect(secure: Boolean) {
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window, secure) {
        val owned = secure && window != null && window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE == 0
        if (owned) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (owned) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@ThemePreviews
@Composable
private fun BiometricScreenPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_biometric_title, subtitle = R.string.sdk_biometric_subtitle, onBack = {}) {
            BiometricContent(
                support = BiometricSupport(hardware = true, enrolled = true, strength = BiometricStrength.STRONG, secureStorage = true),
                fallbackSupported = true,
                allowFallback = true,
                pin = "",
                hasPin = false,
                status = null,
                onAllowFallback = {},
                onVerify = {},
                onPin = {},
                onSave = {},
                onReveal = {},
                onDelete = {},
                onDismissStatus = {},
            )
        }
    }
}
