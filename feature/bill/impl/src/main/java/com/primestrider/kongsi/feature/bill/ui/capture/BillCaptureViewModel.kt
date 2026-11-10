package com.primestrider.kongsi.feature.bill.ui.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.feature.bill.ocr.ParsedReceipt
import com.primestrider.kongsi.feature.bill.ocr.ReceiptParser
import com.primestrider.kongsi.feature.bill.ocr.ReceiptScanException
import com.primestrider.kongsi.feature.bill.ocr.ReceiptScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Why a photo gave nothing to start from. */
enum class ReadFailure {
    /** The photo could not be opened or read at all. */
    UNREADABLE,

    /** Text was read, but no item with a price was found in it. */
    NOTHING_FOUND,
}

/** Where reading a receipt photo stands. */
sealed interface ReadStatus {
    data object Idle : ReadStatus

    data object Reading : ReadStatus

    /** Read: the screen hands [receipt] on to the Review step, then calls `onReadHandled`. */
    data class Read(
        val receipt: ParsedReceipt,
    ) : ReadStatus

    data class Failed(
        val reason: ReadFailure,
    ) : ReadStatus
}

data class BillCaptureUiState(
    val status: ReadStatus = ReadStatus.Idle,
)

/** Capture: reads the chosen receipt photo on the device into items and charges for the Review step. */
@HiltViewModel
class BillCaptureViewModel
    @Inject
    constructor(
        private val scanner: ReceiptScanner,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(BillCaptureUiState())
        val state: StateFlow<BillCaptureUiState> = mutableState.asStateFlow()

        fun onPhoto(uri: Uri) {
            if (mutableState.value.status == ReadStatus.Reading) return
            mutableState.update { it.copy(status = ReadStatus.Reading) }
            viewModelScope.launch {
                val status =
                    try {
                        val receipt = ReceiptParser.parse(scanner.scan(uri))
                        if (receipt.items.isEmpty()) ReadStatus.Failed(ReadFailure.NOTHING_FOUND) else ReadStatus.Read(receipt)
                    } catch (e: ReceiptScanException) {
                        ReadStatus.Failed(ReadFailure.UNREADABLE)
                    }
                mutableState.update { it.copy(status = status) }
            }
        }

        /** Back to choosing a photo after a failure. */
        fun onDismissFailure() {
            mutableState.update { it.copy(status = ReadStatus.Idle) }
        }

        fun onReadHandled() {
            mutableState.update { it.copy(status = ReadStatus.Idle) }
        }
    }
