package com.primestrider.kongsi.feature.bill.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BillDao {
    /** Newest first. Emits after every change to a bill or anything in it. */
    @Transaction
    @Query("SELECT * FROM bills ORDER BY createdAt DESC, id DESC")
    abstract fun observeAll(): Flow<List<BillWithParts>>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id")
    abstract fun observe(id: Long): Flow<BillWithParts?>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id")
    abstract suspend fun get(id: Long): BillWithParts?

    /**
     * Stores [bill] and replaces everything in it with [people] and [items], all or nothing. Rows with id 0 get a new
     * id. [sharers] runs parallel to [items]: the ids of the people who had each item (people already stored, or given
     * here with their id). Returns the bill's id.
     */
    @Transaction
    open suspend fun replace(
        bill: BillEntity,
        people: List<PersonEntity>,
        items: List<ItemEntity>,
        sharers: List<Set<Long>>,
    ): Long {
        require(items.size == sharers.size) { "One set of sharers per item" }
        val inserted = upsertBill(bill)
        val billId = if (bill.id == 0L) inserted else bill.id
        // Removing the old rows also removes their shares (foreign key cascade).
        deleteItems(billId)
        deletePeople(billId)
        val personIds = insertPeople(people.map { it.copy(billId = billId) }).toSet()
        items.forEachIndexed { index, item ->
            val itemId = insertItem(item.copy(billId = billId))
            insertShares(sharers[index].filter { it in personIds }.map { ItemShareEntity(billId, itemId, it) })
        }
        return billId
    }

    @Query("DELETE FROM bills WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /** Inserts (returns the new id) or replaces the row with the same id (returns -1). */
    @Upsert
    protected abstract suspend fun upsertBill(bill: BillEntity): Long

    @Query("DELETE FROM bill_items WHERE billId = :billId")
    protected abstract suspend fun deleteItems(billId: Long)

    @Query("DELETE FROM bill_people WHERE billId = :billId")
    protected abstract suspend fun deletePeople(billId: Long)

    @Insert
    protected abstract suspend fun insertItem(item: ItemEntity): Long

    @Insert
    protected abstract suspend fun insertPeople(people: List<PersonEntity>): List<Long>

    @Insert
    protected abstract suspend fun insertShares(shares: List<ItemShareEntity>)
}
