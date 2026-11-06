package com.primestrider.kotlinstarter.feature.example.sdk.sqlite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity)

    /** Newest first; the id breaks ties between rows created in the same millisecond. */
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun latest(limit: Int = 20): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions")
    fun balance(): Flow<Long>

    @Query("DELETE FROM transactions")
    suspend fun clear()
}
