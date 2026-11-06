package com.primestrider.kotlinstarter.core.network

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Optional
import javax.net.ssl.SSLPeerUnverifiedException

/** The API host is pinned when pins are configured, on both clients; without pins nothing is pinned. */
class PinningTest {
    private val certificate =
        HeldCertificate
            .Builder()
            .addSubjectAlternativeName("localhost")
            .build()
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.useHttps(
            HandshakeCertificates
                .Builder()
                .heldCertificate(certificate)
                .build()
                .sslSocketFactory(),
        )
        server.start()
    }

    @After
    fun tearDown() = server.close()

    private val wrongPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    /** The module's client, made to trust the test certificate (the pinner is kept). */
    private fun trusting(client: OkHttpClient): OkHttpClient {
        val trust = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        return client.newBuilder().sslSocketFactory(trust.sslSocketFactory(), trust.trustManager).build()
    }

    private fun appClient(pins: Set<String>) = trusting(NetworkModule.okHttpClient(emptySet(), Optional.empty(), server.url("/"), pins))

    private fun OkHttpClient.get(): Int {
        server.enqueue(MockResponse(code = 200))
        return newCall(Request.Builder().url(server.url("/")).build()).execute().use { it.code }
    }

    @Test
    fun matching_pin_connects() {
        assertEquals(200, appClient(setOf(CertificatePinner.pin(certificate.certificate), wrongPin)).get())
    }

    /** OkHttp may try the next route after the refused handshake, so the pin failure can be a suppressed cause. */
    private fun assertPinRefused(client: OkHttpClient) {
        val error = assertThrows(IOException::class.java) { client.get() }
        val causes = generateSequence(error as Throwable) { it.cause }.flatMap { sequenceOf(it) + it.suppressed }
        assertTrue(error.toString(), causes.any { it is SSLPeerUnverifiedException })
    }

    @Test
    fun wrong_pin_is_refused() = assertPinRefused(appClient(setOf(wrongPin)))

    @Test
    fun refresh_client_is_pinned_too() = assertPinRefused(trusting(NetworkModule.refreshOkHttpClient(server.url("/"), setOf(wrongPin))))

    @Test
    fun no_pins_means_no_pinner() {
        assertTrue(appClient(emptySet()).certificatePinner.pins.isEmpty())
        assertEquals(200, appClient(emptySet()).get())
    }
}
