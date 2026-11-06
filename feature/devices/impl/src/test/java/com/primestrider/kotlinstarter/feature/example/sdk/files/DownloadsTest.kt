package com.primestrider.kotlinstarter.feature.example.sdk.files

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class DownloadsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var downloads: Downloads

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        downloads = Downloads(folder.root, OkHttpClient())
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun writes_the_body_under_the_downloads_directory() =
        runTest {
            server.enqueue(MockResponse.Builder().body("%PDF-1.4 hello").build())

            val file = downloads.download(server.url("/files/dummy.pdf").toString())

            assertEquals("dummy.pdf", file.name)
            assertEquals(File(folder.root, "downloads"), file.parentFile)
            assertEquals("%PDF-1.4 hello", file.readText())
            assertEquals(listOf("dummy.pdf"), downloads.list().map { it.name })
        }

    @Test
    fun a_server_error_leaves_no_file_behind() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(500)
                    .body("boom")
                    .build(),
            )

            try {
                downloads.download(server.url("/files/dummy.pdf").toString())
                fail("expected an IOException")
            } catch (expected: IOException) {
                // download failed as intended
            }

            assertTrue(downloads.list().isEmpty())
            assertFalse(File(folder.root, "downloads").listFiles().orEmpty().any { it.name.endsWith(".part") })
        }

    @Test
    fun clear_empties_the_directory() =
        runTest {
            server.enqueue(MockResponse.Builder().body("x").build())
            downloads.download(server.url("/a.pdf").toString())
            downloads.clear()
            assertTrue(downloads.list().isEmpty())
        }
}
