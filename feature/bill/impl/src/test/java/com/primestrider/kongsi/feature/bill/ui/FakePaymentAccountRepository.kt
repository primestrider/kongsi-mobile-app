package com.primestrider.kongsi.feature.bill.ui

import android.database.sqlite.SQLiteException
import com.primestrider.kongsi.feature.bill.data.PaymentAccountRepository
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** In-memory [PaymentAccountRepository]; [failWrites] makes every write throw like a full disk. */
class FakePaymentAccountRepository(
    initial: List<PaymentAccount> = emptyList(),
) : PaymentAccountRepository {
    private val stored = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1
    var failWrites = false

    override val accounts: StateFlow<List<PaymentAccount>> = stored

    override suspend fun get(id: Long): PaymentAccount? = stored.value.firstOrNull { it.id == id }

    override suspend fun save(account: PaymentAccount): Long {
        if (failWrites) throw SQLiteException("disk full")
        val saved = if (account.id == 0L) account.copy(id = nextId, createdAt = 1_000 + nextId++) else account
        stored.update { list -> (list.filterNot { it.id == saved.id } + saved).sortedBy { it.createdAt } }
        return saved.id
    }

    override suspend fun delete(id: Long): PaymentAccount? {
        if (failWrites) throw SQLiteException("disk full")
        return get(id)?.also { removed -> stored.update { list -> list.filterNot { it.id == removed.id } } }
    }
}
