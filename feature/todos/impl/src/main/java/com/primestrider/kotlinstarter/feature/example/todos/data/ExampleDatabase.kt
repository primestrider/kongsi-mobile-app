package com.primestrider.kotlinstarter.feature.example.todos.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The todos feature's own database (`example.db`), so deleting the feature deletes it. Bump [version] together
 * with a `Migration`; schemas are exported to `feature/todos/impl/schemas`.
 */
@Database(entities = [TodoEntity::class], version = 3, exportSchema = true)
abstract class ExampleDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}

/** v1 to v2: added the devices demo's `transactions` table (dropped again in v3); `todos` is untouched. */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `transactions` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kind` TEXT NOT NULL, " +
                    "`amount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
            )
        }
    }

/**
 * v2 to v3: the devices demo's `transactions` table moved to that feature's own `devices.db`, so it is dropped
 * here; `todos` is untouched. The demo's old rows are not carried over (example data).
 */
val MIGRATION_2_3: Migration =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `transactions`")
        }
    }
