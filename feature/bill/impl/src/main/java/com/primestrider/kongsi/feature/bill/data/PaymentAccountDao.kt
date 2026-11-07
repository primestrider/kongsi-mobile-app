package com.primestrider.kongsi.feature.bill.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentAccountDao {
    /** Oldest first, so the order the user added them is the order in the message. Emits after every change. */
    @Query("SELECT * FROM payment_accounts ORDER BY createdAt, id")
    fun observeAll(): Flow<List<PaymentAccountEntity>>

    @Query("SELECT * FROM payment_accounts WHERE id = :id")
    suspend fun get(id: Long): PaymentAccountEntity?

    /** Inserts (returns the new id) or replaces the row with the same id (returns -1). */
    @Upsert
    suspend fun upsert(account: PaymentAccountEntity): Long

    @Query("DELETE FROM payment_accounts WHERE id = :id")
    suspend fun delete(id: Long)
}
