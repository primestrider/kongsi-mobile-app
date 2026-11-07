package com.primestrider.kongsi.feature.bill.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The bill feature's database (`kongsi.db`). Bills, items and people join it in later versions: bump [version]
 * together with a `Migration`; schemas are exported to `feature/bill/impl/schemas`.
 */
@Database(entities = [PaymentAccountEntity::class], version = 1, exportSchema = true)
abstract class KongsiDatabase : RoomDatabase() {
    abstract fun paymentAccountDao(): PaymentAccountDao
}
