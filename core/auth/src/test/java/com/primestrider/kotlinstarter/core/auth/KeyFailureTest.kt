package com.primestrider.kotlinstarter.core.auth

import android.security.keystore.KeyPermanentlyInvalidatedException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyStoreException
import java.security.UnrecoverableKeyException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KeyFailureTest {
    @Test
    fun permanentFailuresAreRecognised() {
        assertTrue(isPermanentKeyFailure(KeyPermanentlyInvalidatedException()))
        assertTrue(isPermanentKeyFailure(UnrecoverableKeyException("gone")))
    }

    @Test
    fun transientFailuresAreNotPermanent() {
        assertFalse(isPermanentKeyFailure(InvalidKeyException("Keystore operation failed")))
        assertFalse(isPermanentKeyFailure(KeyStoreException("busy")))
        assertFalse(isPermanentKeyFailure(GeneralSecurityException("tag mismatch")))
    }
}
