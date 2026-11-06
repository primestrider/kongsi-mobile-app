package com.primestrider.kotlinstarter.feature.example.sdk.biometric

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "example_pin_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val PREFS_NAME = "example_pin"
private const val CIPHERTEXT_KEY = "ciphertext"
private const val IV_KEY = "iv"
private const val DEVICE_CREDENTIAL_KEY = "device_credential"

/** The encrypted PIN as stored, and whether its key also accepts the device credential (fixed when the key was created). */
class StoredPin(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val allowsDeviceCredential: Boolean,
)

/** Where [PinVault] keeps the ciphertext; split out so the layout is testable without a Keystore. */
interface PinStore {
    fun read(): StoredPin?

    fun write(pin: StoredPin)

    fun clear()
}

/** [PinStore] over `SharedPreferences("example_pin")`: Base64 ciphertext and IV; the PIN itself is never stored. */
class PrefsPinStore(
    context: Context,
) : PinStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): StoredPin? {
        val ciphertext = prefs.getString(CIPHERTEXT_KEY, null) ?: return null
        val iv = prefs.getString(IV_KEY, null) ?: return null
        return StoredPin(
            Base64.decode(ciphertext, Base64.NO_WRAP),
            Base64.decode(iv, Base64.NO_WRAP),
            prefs.getBoolean(DEVICE_CREDENTIAL_KEY, false),
        )
    }

    override fun write(pin: StoredPin) {
        prefs
            .edit()
            .putString(CIPHERTEXT_KEY, Base64.encodeToString(pin.ciphertext, Base64.NO_WRAP))
            .putString(IV_KEY, Base64.encodeToString(pin.iv, Base64.NO_WRAP))
            .putBoolean(DEVICE_CREDENTIAL_KEY, pin.allowsDeviceCredential)
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}

/** A cipher to unlock through `BiometricPrompt.CryptoObject`; the prompt must offer the device credential only when the key allows it. */
class PinCipher(
    val cipher: Cipher,
    val allowsDeviceCredential: Boolean,
)

/**
 * A PIN encrypted with an AES/GCM Keystore key ([KEY_ALIAS]) that needs user authentication for every use, so the cipher only
 * works after a `BiometricPrompt`. The key is invalidated when biometrics are enrolled or removed: [encryptCipher] and
 * [decryptCipher] then throw `KeyPermanentlyInvalidatedException`, and the caller should [delete]. Every call may throw a
 * `GeneralSecurityException` or `IllegalStateException` (no Keystore, nothing enrolled); callers turn those into a status.
 */
class PinVault(
    context: Context,
    private val store: PinStore = PrefsPinStore(context),
) {
    fun hasPin(): Boolean = store.read() != null

    /**
     * A cipher for saving. It reuses the key of the stored PIN; with no PIN a new key is made, accepting the device
     * credential too when [allowDeviceCredential] and the device runs Android 11+ (biometric only below).
     */
    fun encryptCipher(allowDeviceCredential: Boolean): PinCipher {
        val stored = store.read()
        val existing = stored?.let { existingKey() }
        val allowsDeviceCredential =
            stored?.takeIf { existing != null }?.allowsDeviceCredential
                ?: (allowDeviceCredential && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        val key = existing ?: createKey(allowsDeviceCredential)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
        return PinCipher(cipher, allowsDeviceCredential)
    }

    /** A cipher for reading the stored PIN, or null when there is none (or its key is gone, which also drops the PIN). */
    fun decryptCipher(): PinCipher? {
        val stored = store.read() ?: return null
        val key = existingKey()
        if (key == null) {
            store.clear()
            return null
        }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, stored.iv)) }
        return PinCipher(cipher, stored.allowsDeviceCredential)
    }

    /** Encrypts [value] with an authenticated [cipher] from [encryptCipher] and stores it. */
    fun save(
        cipher: PinCipher,
        value: String,
    ) {
        val ciphertext = cipher.cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        store.write(StoredPin(ciphertext, cipher.cipher.iv, cipher.allowsDeviceCredential))
    }

    /** Decrypts the stored PIN with an authenticated [cipher] from [decryptCipher]. */
    fun read(cipher: Cipher): String? = store.read()?.let { String(cipher.doFinal(it.ciphertext), Charsets.UTF_8) }

    /** Removes the PIN and its key. */
    fun delete() {
        store.clear()
        try {
            keyStore().deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {
            // No Keystore or no key: nothing left to delete.
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun createKey(allowDeviceCredential: Boolean): SecretKey {
        val spec =
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val types =
                            if (allowDeviceCredential) {
                                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                            } else {
                                KeyProperties.AUTH_BIOMETRIC_STRONG
                            }
                        setUserAuthenticationParameters(0, types)
                    }
                }.build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(spec)
            generateKey()
        }
    }

    companion object {
        /** Whether the Android Keystore can be opened on this device. */
        fun isKeystoreAvailable(): Boolean =
            try {
                KeyStore.getInstance(KEYSTORE).load(null)
                true
            } catch (_: Exception) {
                false
            }
    }
}
