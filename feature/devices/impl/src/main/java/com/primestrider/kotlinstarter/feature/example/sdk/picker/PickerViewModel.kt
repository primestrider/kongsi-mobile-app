package com.primestrider.kotlinstarter.feature.example.sdk.picker

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kotlinstarter.feature.example.sdk.files.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

enum class MediaSource { IMAGES, VIDEOS }

/** MIME filter of the document picker. */
enum class DocumentType(
    val mime: String,
) {
    ANY("*/*"),
    PDF("application/pdf"),
    IMAGES("image/*"),
}

/** The picker's options; [crop] only applies to a single image, [quality] (0.1..1) to the JPEG re-encode. */
data class PickerOptions(
    val source: MediaSource = MediaSource.IMAGES,
    val crop: Boolean = true,
    val multiple: Boolean = false,
    val quality: Float = 0.8f,
)

enum class PickerError { CAMERA_DENIED, NO_APP, READ_FAILED }

data class PickerState(
    val media: List<PickedMedia> = emptyList(),
    val documents: List<PickedDocument> = emptyList(),
    val error: PickerError? = null,
)

/** Reads what the pickers return off the main thread: images are re-encoded (and cropped), videos and documents are only measured. */
@HiltViewModel
class PickerViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _state = MutableStateFlow(PickerState())
        val state: StateFlow<PickerState> = _state.asStateFlow()
        private var job: Job? = null

        /** Library or camera result; each pick replaces the list. */
        fun onMedia(
            uris: List<Uri>,
            options: PickerOptions,
        ) {
            if (uris.isEmpty()) return
            job?.cancel()
            job =
                viewModelScope.launch {
                    _state.update { it.copy(error = null) }
                    try {
                        val media = withContext(Dispatchers.IO) { uris.map { read(it, options, uris.size) } }
                        _state.update { it.copy(media = media) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        fail(PickerError.READ_FAILED)
                    } catch (e: OutOfMemoryError) {
                        fail(PickerError.READ_FAILED)
                    }
                }
        }

        fun onDocuments(uris: List<Uri>) {
            if (uris.isEmpty()) return
            job?.cancel()
            job =
                viewModelScope.launch {
                    val documents = withContext(Dispatchers.IO) { uris.map { context.contentResolver.readDocument(it) } }
                    _state.update { it.copy(documents = documents, error = null) }
                }
        }

        fun fail(error: PickerError) = _state.update { it.copy(error = error) }

        private fun read(
            uri: Uri,
            options: PickerOptions,
            count: Int,
        ): PickedMedia {
            val resolver = context.contentResolver
            if (options.source == MediaSource.VIDEOS) return resolver.readVideo(uri) ?: throw IOException("Not a video")
            val out = File(context.cacheDir, "picker/${System.nanoTime()}.jpg")
            // Re-encoded at the full width; a single image can then be cropped to a square.
            checkNotNull(resolver.openInputStream(uri)) { "Cannot open $uri" }.let {
                ImageCompressor.compress(it, Int.MAX_VALUE, options.quality, out)
            }
            if (options.crop && !options.multiple && count == 1) cropToSquare(out, options.quality)
            return resolver.readImage(out.toUri()) ?: throw IOException("Not an image")
        }

        override fun onCleared() {
            job?.cancel()
        }
    }
