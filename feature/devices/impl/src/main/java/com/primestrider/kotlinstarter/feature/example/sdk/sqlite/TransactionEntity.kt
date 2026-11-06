package com.primestrider.kotlinstarter.feature.example.sdk.sqlite

import androidx.room.Entity
import androidx.room.PrimaryKey

/** What a transaction does to the balance: [sign] multiplies the amount the user picked. */
enum class TransactionKind(
    val sign: Int,
) {
    TOP_UP(+1),
    PAYMENT(-1),
    TRANSFER(-1),
}

/** One balance movement; [amount] is signed (negative for money going out) so the balance is a plain SUM. */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val amount: Long,
    val createdAt: Long,
)
