package com.primestrider.kotlinstarter.feature.example.sdk.picker

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PickerPipelineTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val models = mutableListOf<PickerViewModel>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        // The IO work of a view model must not outlive the test and touch Dispatchers.Main later.
        models.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun image(
        name: String,
        width: Int,
        height: Int,
        format: Bitmap.CompressFormat,
    ): File =
        folder.newFile(name).also { file ->
            file.outputStream().use { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(format, 100, it) }
        }

    private fun newModel() = PickerViewModel(app).also { models += it }

    private fun PickerViewModel.await(done: (PickerState) -> Boolean): PickerState =
        runBlocking { withTimeout(10_000) { state.first(done) } }

    private fun boundsOf(file: File) =
        BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeFile(file.path, it) }

    @Test
    fun read_image_reports_real_size_and_mime_of_a_file_uri() {
        val file = image("photo.png", 640, 480, Bitmap.CompressFormat.PNG)
        val media = app.contentResolver.readImage(Uri.fromFile(file))
        assertEquals(640 to 480, media?.width to media?.height)
        assertEquals("image/png", media?.mime)
        assertEquals(file.length(), media?.bytes)
    }

    @Test
    fun crop_to_square_keeps_the_shorter_side() {
        val file = image("wide.jpg", 2000, 1000, Bitmap.CompressFormat.JPEG)
        assertEquals(1000, cropToSquare(file, 0.8f))
        val bounds = boundsOf(file)
        assertEquals(1000 to 1000, bounds.outWidth to bounds.outHeight)
    }

    @Test
    fun a_single_image_is_cropped_to_a_square() {
        val file = image("one.png", 2000, 1000, Bitmap.CompressFormat.PNG)
        val vm = newModel()
        vm.onMedia(listOf(Uri.fromFile(file)), PickerOptions(crop = true))
        val media = vm.await { it.media.isNotEmpty() }.media.single()
        assertEquals(1000 to 1000, media.width to media.height)
        assertEquals("image/jpeg", media.mime)
    }

    @Test
    fun several_images_are_not_cropped() {
        val a = image("a.png", 2000, 1000, Bitmap.CompressFormat.PNG)
        val b = image("b.png", 1000, 2000, Bitmap.CompressFormat.PNG)
        val vm = newModel()
        vm.onMedia(listOf(Uri.fromFile(a), Uri.fromFile(b)), PickerOptions(crop = true, multiple = true))
        val media = vm.await { it.media.size == 2 }.media
        assertEquals(listOf(2000 to 1000, 1000 to 2000), media.map { it.width to it.height })
    }

    @Test
    fun crop_off_keeps_the_original_proportions() {
        val file = image("keep.png", 2000, 1000, Bitmap.CompressFormat.PNG)
        val vm = newModel()
        vm.onMedia(listOf(Uri.fromFile(file)), PickerOptions(crop = false))
        val media = vm.await { it.media.isNotEmpty() }.media.single()
        assertEquals(2000 to 1000, media.width to media.height)
    }

    @Test
    fun an_unreadable_file_fails_with_read_failed() {
        val file = File(folder.root, "missing.jpg")
        val vm = newModel()
        vm.onMedia(listOf(Uri.fromFile(file)), PickerOptions())
        val state = vm.await { it.error != null }
        assertEquals(PickerError.READ_FAILED, state.error)
        assertTrue(state.media.isEmpty())
    }

    @Test
    fun a_new_pick_replaces_the_previous_list_and_clears_the_error() {
        val first = image("first.png", 2000, 1000, Bitmap.CompressFormat.PNG)
        val second = image("second.png", 800, 600, Bitmap.CompressFormat.PNG)
        val bad = File(folder.root, "missing.jpg")
        val vm = newModel()
        vm.onMedia(listOf(Uri.fromFile(first)), PickerOptions(crop = false))
        vm.await { it.media.size == 1 && it.media[0].width == 2000 }
        vm.onMedia(listOf(Uri.fromFile(bad)), PickerOptions())
        vm.await { it.error != null }
        vm.onMedia(listOf(Uri.fromFile(second)), PickerOptions(crop = false))
        val state = vm.await { it.media.size == 1 && it.media[0].width == 800 && it.error == null }
        assertEquals(800 to 600, state.media[0].width to state.media[0].height)
    }

    @Test
    fun thumbnails_are_scaled_down_but_never_up() {
        val big = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val scaled = scaleThumbnail(big, 208)
        assertEquals(208, minOf(scaled.width, scaled.height))
        val small = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        assertEquals(small, scaleThumbnail(small, 208))
    }
}
