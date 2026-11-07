package com.primestrider.kongsi.feature.bill.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.feature.bill.data.PaymentAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BillHomeUiState(
    /** True once the database has answered and no payment account is saved yet. */
    val showAccountHint: Boolean = false,
)

@HiltViewModel
class BillHomeViewModel
    @Inject
    constructor(
        repository: PaymentAccountRepository,
    ) : ViewModel() {
        val state: StateFlow<BillHomeUiState> =
            repository.accounts
                .map { BillHomeUiState(showAccountHint = it.isEmpty()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillHomeUiState())
    }
