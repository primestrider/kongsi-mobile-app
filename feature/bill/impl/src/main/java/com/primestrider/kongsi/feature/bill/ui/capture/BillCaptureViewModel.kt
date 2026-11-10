package com.primestrider.kongsi.feature.bill.ui.capture

import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.data.BillRepository
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.ocr.ReceiptParser
import com.primestrider.kongsi.feature.bill.ocr.ReceiptScanException
import com.primestrider.kongsi.feature.bill.ocr.ReceiptScanner
import com.primestrider.kongsi.feature.bill.ocr.toDraftBill
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

    /** Read and saved as the draft [billId]: the screen opens it on the Review step, then calls `onReadHandled`. */
    data class Read(
        val billId: Long,
    ) : ReadStatus

    data class Failed(
        val reason: ReadFailure,
    ) : ReadStatus
}

data class BillCaptureUiState(
    val status: ReadStatus = ReadStatus.Idle,
)

/**
 * Capture: reads the chosen receipt photo on the device into items and charges, and saves them as a draft bill for the
 * Review step, where the user checks what was read.
 */
@HiltViewModel
class BillCaptureViewModel
    @Inject
    constructor(
        private val scanner: ReceiptScanner,
        private val bills: BillRepository,
        private val messenger: Messenger,
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
                        if (receipt.items.isEmpty()) {
                            ReadStatus.Failed(ReadFailure.NOTHING_FOUND)
                        } else {
                            ReadStatus.Read(bills.save(receipt.toDraftBill()))
                        }
                    } catch (e: ReceiptScanException) {
                        ReadStatus.Failed(ReadFailure.UNREADABLE)
                    } catch (e: SQLiteException) {
                        messenger.error(UiText.Resource(R.string.bill_save_failed))
                        ReadStatus.Idle
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
