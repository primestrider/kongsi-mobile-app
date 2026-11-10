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
 * - v3: people the user split bills with before (suggestions on the People step).
 * - v4: the payment accounts chosen for each bill.
 * - v5: who on a bill has paid.
 */
@Database(
    entities = [
        PaymentAccountEntity::class,
        BillEntity::class,
        ItemEntity::class,
        PersonEntity::class,
        ItemShareEntity::class,
        FrequentPersonEntity::class,
        BillPaymentAccountEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
        ), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5),
    ],
)
abstract class KongsiDatabase : RoomDatabase() {
    abstract fun paymentAccountDao(): PaymentAccountDao

    abstract fun billDao(): BillDao

    abstract fun frequentPersonDao(): FrequentPersonDao
}
