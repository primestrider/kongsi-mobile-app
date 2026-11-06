package com.primestrider.kotlinstarter.feature.example.sdk.files

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt

private const val MAX_WIDTH_KEY = "maxWidth"
private const val QUALITY_KEY = "quality"
private const val PICKED_KEY = "picked"
const val DEFAULT_MAX_WIDTH = 1080
const val DEFAULT_QUALITY = 0.6f
const val SAMPLE_PDF_URL = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"

data class FilesUiState(
    val maxWidth: Int = DEFAULT_MAX_WIDTH,
    val quality: Float = DEFAULT_QUALITY,
    val picked: Uri? = null,
    val compressing: Boolean = false,
    val result: CompressedImage? = null,
    val compressFailed: Boolean = false,
    val downloading: Boolean = false,
    val downloads: List<File> = emptyList(),
    val downloadFailed: Boolean = false,
)

/** Compress settings and the picked photo live in [SavedStateHandle]; the result and the file list are rebuilt on demand. */
@HiltViewModel
class FilesViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        client: OkHttpClient,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val downloads = Downloads(context.cacheDir, client)
        private val work = MutableStateFlow(FilesUiState(downloads = downloads.list()))

        val state: StateFlow<FilesUiState> =
            combine(
                savedStateHandle.getStateFlow(MAX_WIDTH_KEY, DEFAULT_MAX_WIDTH),
                savedStateHandle.getStateFlow(QUALITY_KEY, DEFAULT_QUALITY),
                savedStateHandle.getStateFlow<String?>(PICKED_KEY, null),
                work,
            ) { maxWidth, quality, picked, work ->
                work.copy(maxWidth = maxWidth, quality = quality, picked = picked?.let(Uri::parse))
            }.stateIn(viewModelScope, SharingStarted.Eagerly, work.value)

        fun setMaxWidth(value: Int) {
            savedStateHandle[MAX_WIDTH_KEY] = value
        }

        fun setQuality(value: Float) {
            savedStateHandle[QUALITY_KEY] = (value * 10).roundToInt() / 10f
        }

        fun pick(uri: Uri) {
            savedStateHandle[PICKED_KEY] = uri.toString()
            work.update { it.copy(result = null, compressFailed = false) }
        }

        fun compress() {
            val uri = state.value.picked ?: return
            val (maxWidth, quality) = state.value.maxWidth to state.value.quality
            work.update { it.copy(compressing = true, compressFailed = false) }
            viewModelScope.launch {
                val outcome =
                    try {
                        withContext(Dispatchers.IO) { compressTo(uri, maxWidth, quality) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: IOException) {
                        null
                    } catch (e: IllegalArgumentException) {
                        null
                    } catch (e: IllegalStateException) {
                        null
                    } catch (e: SecurityException) {
                        null
                    }
                work.update { it.copy(compressing = false, result = outcome, compressFailed = outcome == null) }
            }
        }

        private fun compressTo(
            uri: Uri,
            maxWidth: Int,
            quality: Float,
        ): CompressedImage {
            val dir = File(context.cacheDir, "compressed")
            // A new name per run: the preview is keyed by path, so reusing one would show the previous image.
            dir.listFiles().orEmpty().forEach { it.delete() }
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
            return ImageCompressor.compress(input, maxWidth, quality, File(dir, "compressed-${System.currentTimeMillis()}.jpg"))
        }

        fun download() {
            work.update { it.copy(downloading = true, downloadFailed = false) }
            viewModelScope.launch {
                val failed =
                    try {
                        downloads.download(SAMPLE_PDF_URL)
                        false
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: IOException) {
                        true
                    }
                work.update { it.copy(downloading = false, downloadFailed = failed, downloads = downloads.list()) }
            }
        }

        fun clearDownloads() {
            downloads.clear()
            work.update { it.copy(downloads = downloads.list(), downloadFailed = false) }
        }
    }
