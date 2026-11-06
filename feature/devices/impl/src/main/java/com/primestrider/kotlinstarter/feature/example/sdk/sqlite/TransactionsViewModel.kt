package com.primestrider.kotlinstarter.feature.example.sdk.sqlite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Not "amount": SdkDemoRoute has an `amount` argument, which the navigation puts in this same SavedStateHandle.
private const val KIND_KEY = "selected_kind"
private const val AMOUNT_KEY = "selected_amount"
private const val DEFAULT_AMOUNT = 50_000L

data class TransactionsUiState(
    val kind: TransactionKind = TransactionKind.TOP_UP,
    val amount: Long = DEFAULT_AMOUNT,
    val items: List<TransactionEntity> = emptyList(),
    val balance: Long = 0,
    /** False until Room has answered once, so "No transactions yet." does not flash on open. */
    val loaded: Boolean = false,
)

/** Transaction history in Room; the picked kind and amount live in [SavedStateHandle] (rotation, process death). */
@HiltViewModel
class TransactionsViewModel(
    private val dao: TransactionDao,
    private val savedStateHandle: SavedStateHandle,
    private val clock: () -> Long,
) : ViewModel() {
    @Inject
    constructor(dao: TransactionDao, savedStateHandle: SavedStateHandle) : this(dao, savedStateHandle, System::currentTimeMillis)

    val state: StateFlow<TransactionsUiState> =
        combine(
            savedStateHandle.getStateFlow(KIND_KEY, TransactionKind.TOP_UP),
            savedStateHandle.getStateFlow(AMOUNT_KEY, DEFAULT_AMOUNT),
            // null until Room answers, so the selection never waits for the first query.
            combine(dao.latest(), dao.balance(), ::Pair).onStart<Pair<List<TransactionEntity>, Long>?> { emit(null) },
        ) { kind, amount, stored ->
            TransactionsUiState(kind, amount, stored?.first.orEmpty(), stored?.second ?: 0, loaded = stored != null)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            TransactionsUiState(
                kind = savedStateHandle[KIND_KEY] ?: TransactionKind.TOP_UP,
                amount = savedStateHandle[AMOUNT_KEY] ?: DEFAULT_AMOUNT,
            ),
        )

    fun selectKind(kind: TransactionKind) {
        savedStateHandle[KIND_KEY] = kind
    }

    fun selectAmount(amount: Long) {
        savedStateHandle[AMOUNT_KEY] = amount
    }

    fun add() {
        val kind = savedStateHandle[KIND_KEY] ?: TransactionKind.TOP_UP
        val amount = savedStateHandle[AMOUNT_KEY] ?: DEFAULT_AMOUNT
        viewModelScope.launch { dao.insert(TransactionEntity(kind = kind.name, amount = kind.sign * amount, createdAt = clock())) }
    }

    fun clear() {
        viewModelScope.launch { dao.clear() }
    }
}
