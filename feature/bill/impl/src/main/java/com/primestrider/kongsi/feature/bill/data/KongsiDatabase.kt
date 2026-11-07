package com.primestrider.kongsi.feature.bill.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The bill feature's database (`kongsi.db`). Schemas are exported to `feature/bill/impl/schemas`; bump [version]
 * together with a migration (an auto-migration when tables or columns are only added) and cover it in
 * `KongsiDatabaseMigrationTest`.
 *
 * - v1: payment accounts.
 * - v2: bills, their items, people and who had which item.
 */
@Database(
    entities = [
        PaymentAccountEntity::class,
        BillEntity::class,
        ItemEntity::class,
        PersonEntity::class,
        ItemShareEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class KongsiDatabase : RoomDatabase() {
    abstract fun paymentAccountDao(): PaymentAccountDao

    abstract fun billDao(): BillDao
}
