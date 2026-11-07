package com.primestrider.kongsi.feature.bill.ui

import android.database.sqlite.SQLiteException
import com.primestrider.kongsi.feature.bill.data.BillRepository
import com.primestrider.kongsi.feature.bill.model.Bill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [BillRepository], newest first like Room; [failWrites] makes every write throw like a full disk. */
class FakeBillRepository(
    initial: List<Bill> = emptyList(),
) : BillRepository {
    private val stored = MutableStateFlow(initial.sortedByDescending { it.createdAt })
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1
    var failWrites = false

    override val bills: StateFlow<List<Bill>> = stored

    override fun observe(id: Long): Flow<Bill?> = stored.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun get(id: Long): Bill? = stored.value.firstOrNull { it.id == id }

    override suspend fun save(bill: Bill): Long {
        if (failWrites) throw SQLiteException("disk full")
        val saved = if (bill.id == 0L) bill.copy(id = nextId, createdAt = 1_000 + nextId++) else bill
        stored.update { list -> (list.filterNot { it.id == saved.id } + saved).sortedByDescending { it.createdAt } }
        return saved.id
    }

    override suspend fun delete(id: Long): Bill? {
        if (failWrites) throw SQLiteException("disk full")
        return get(id)?.also { removed -> stored.update { list -> list.filterNot { it.id == removed.id } } }
    }
}
