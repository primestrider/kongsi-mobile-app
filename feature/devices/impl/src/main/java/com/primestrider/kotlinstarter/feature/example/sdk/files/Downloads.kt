package com.primestrider.kotlinstarter.feature.example.sdk.files

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Files downloaded into `cache/downloads`, which the OS may purge. Uses the app client; the bearer is only added for
 * the API host, so a download from another host carries no credentials.
 */
class Downloads(
    cacheDir: File,
    private val client: OkHttpClient,
) {
    val dir: File = File(cacheDir, "downloads")

    /** Downloads [url] to `<name>.part` and renames it when complete, so a failure never leaves a half file. */
    suspend fun download(url: String): File =
        withContext(Dispatchers.IO) {
            dir.mkdirs()
            val name = url.substringBefore('?').substringAfterLast('/').ifBlank { "download" }
            val target = File(dir, name)
            val part = File(dir, "$name.part")
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body
                    part.outputStream().use { body.byteStream().copyTo(it) }
                }
                if (target.exists()) target.delete()
                if (!part.renameTo(target)) throw IOException("Could not save $name")
                target
            } finally {
                part.delete()
            }
        }

    /** Completed downloads, sorted by name; in-progress `.part` files are not listed. */
    fun list(): List<File> = dir.listFiles { file -> file.isFile && !file.name.endsWith(".part") }.orEmpty().sortedBy { it.name }

    fun clear() {
        dir.listFiles().orEmpty().forEach { it.delete() }
    }
}
