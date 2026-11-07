package com.primestrider.kongsi.feature.bill.ui.accounts

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.common.ApplicationScope
import com.primestrider.kongsi.core.designsystem.component.MessageVariant
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiMessage
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.data.PaymentAccountRepository
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentAccountsUiState(
    val accounts: List<PaymentAccount> = emptyList(),
    /** False until the database has answered once, so the empty state does not flash on open. */
    val loaded: Boolean = false,
)

/**
 * The saved payment accounts. Delete happens at once and offers Undo; the undo runs in [appScope], so it still
 * works after the user has left the screen while the message is showing.
 */
@HiltViewModel
class PaymentAccountsViewModel
    @Inject
    constructor(
        private val repository: PaymentAccountRepository,
        private val messenger: Messenger,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        val state: StateFlow<PaymentAccountsUiState> =
            repository.accounts
                .map { PaymentAccountsUiState(accounts = it, loaded = true) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaymentAccountsUiState())

        fun delete(id: Long) {
            viewModelScope.launch {
                val removed =
                    try {
                        repository.delete(id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: SQLiteException) {
                        messenger.error(UiText.Resource(R.string.bill_account_save_failed))
                        null
                    } ?: return@launch
                messenger.show(
                    UiMessage(
                        text = UiText.Resource(R.string.bill_account_deleted, listOf(removed.provider)),
                        variant = MessageVariant.INFO,
                        actionLabel = UiText.Resource(R.string.bill_undo),
                        onAction = { appScope.launch { restore(removed) } },
                    ),
                )
            }
        }

        private suspend fun restore(account: PaymentAccount) {
            try {
                repository.save(account)
            } catch (e: SQLiteException) {
                messenger.error(UiText.Resource(R.string.bill_account_save_failed))
            }
        }
    }
