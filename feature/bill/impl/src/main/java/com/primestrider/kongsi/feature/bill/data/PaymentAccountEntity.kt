package com.primestrider.kongsi.feature.bill.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_accounts")
data class PaymentAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    /** A `PaymentAccountType` name. */
    val type: String,
    val provider: String,
    val number: String,
    val holderName: String,
    val isDefault: Boolean,
    val createdAt: Long,
)
