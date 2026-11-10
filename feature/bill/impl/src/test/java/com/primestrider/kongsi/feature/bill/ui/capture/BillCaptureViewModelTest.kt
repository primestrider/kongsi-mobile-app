package com.primestrider.kongsi.feature.bill.ui.capture

import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.feature.bill.ocr.ParsedItem
import com.primestrider.kongsi.feature.bill.ocr.ReceiptLine
import com.primestrider.kongsi.feature.bill.ocr.ReceiptScanException
import com.primestrider.kongsi.feature.bill.ocr.ReceiptScanner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class BillCaptureViewModelTest {
    private val main = StandardTestDispatcher()
    private val photo: Uri = Uri.parse("content://app.receipts/receipts/receipt-1.jpg")

    /** Returns [lines], or throws when [fails]; records each photo it was given. */
    private class FakeScanner(
        var lines: List<ReceiptLine> = emptyList(),
        var fails: Boolean = false,
    ) : ReceiptScanner {
        val scanned = mutableListOf<Uri>()

        override suspend fun scan(uri: Uri): List<ReceiptLine> {
            scanned += uri
            if (fails) throw ReceiptScanException(IllegalStateException("model not ready"))
            return lines
        }
    }

    private val scanner = FakeScanner()
    private lateinit var viewModel: BillCaptureViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(main)
        viewModel = BillCaptureViewModel(scanner)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    private fun row(
        index: Int,
        text: String,
    ) = ReceiptLine(text, left = 0, top = index * 40, right = 300, bottom = index * 40 + 30)

    @Test
    fun a_photo_is_read_into_a_receipt() =
        runTest(main) {
            scanner.lines = listOf(row(0, "Nasi Goreng 30.000"), row(1, "Total 30.000"))

            viewModel.onPhoto(photo)
            advanceUntilIdle()

            val status = viewModel.state.value.status as ReadStatus.Read
            assertEquals(listOf(ParsedItem("Nasi Goreng", 1, 30_000)), status.receipt.items)
            assertEquals(30_000L, status.receipt.totalMinor)
            assertEquals(listOf(photo), scanner.scanned)
        }

    @Test
    fun reading_shows_while_the_photo_is_read() =
        runTest(main) {
            val slow = CompletableDeferred<Unit>()
            val scanner =
                object : ReceiptScanner {
                    override suspend fun scan(uri: Uri): List<ReceiptLine> {
                        slow.await()
                        return emptyList()
                    }
                }
            val viewModel = BillCaptureViewModel(scanner)

            viewModel.onPhoto(photo)
            advanceUntilIdle()
            assertEquals(ReadStatus.Reading, viewModel.state.value.status)

            slow.complete(Unit)
            advanceUntilIdle()
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun a_photo_without_any_item_says_nothing_was_found() =
        runTest(main) {
            scanner.lines = listOf(row(0, "Terima kasih"))

            viewModel.onPhoto(photo)
            advanceUntilIdle()

            assertEquals(ReadStatus.Failed(ReadFailure.NOTHING_FOUND), viewModel.state.value.status)
        }

    @Test
    fun a_photo_that_cannot_be_read_says_so() =
        runTest(main) {
            scanner.fails = true

            viewModel.onPhoto(photo)
            advanceUntilIdle()

            assertEquals(ReadStatus.Failed(ReadFailure.UNREADABLE), viewModel.state.value.status)
        }

    @Test
    fun after_a_failure_the_user_can_try_again() =
        runTest(main) {
            scanner.fails = true
            viewModel.onPhoto(photo)
            advanceUntilIdle()

            viewModel.onDismissFailure()

            assertEquals(ReadStatus.Idle, viewModel.state.value.status)
        }

    @Test
    fun a_read_receipt_is_handed_over_once() =
        runTest(main) {
            scanner.lines = listOf(row(0, "Nasi Goreng 30.000"))
            viewModel.onPhoto(photo)
            advanceUntilIdle()

            viewModel.onReadHandled()

            assertEquals(ReadStatus.Idle, viewModel.state.value.status)
        }
}
