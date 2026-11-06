package com.primestrider.kotlinstarter.core.auth

/** XOR with a constant byte: ciphertext differs from plaintext, no Keystore needed on the JVM. */
class FakeSessionCipher : SessionCipher {
    override fun encrypt(plain: ByteArray): ByteArray = ByteArray(plain.size) { (plain[it].toInt() xor KEY).toByte() }

    override fun decrypt(cipher: ByteArray): ByteArray = ByteArray(cipher.size) { (cipher[it].toInt() xor KEY).toByte() }

    private companion object {
        const val KEY = 0x5A
    }
}
