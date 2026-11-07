package com.primestrider.kongsi.feature.bill.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.primestrider.kongsi.feature.bill.impl.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Upgrades keep saved payment accounts. Reads the exported schemas in `feature/bill/impl/schemas` (debug assets). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class KongsiDatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = File.createTempFile("kongsi-migration-test", ".db"),
            driver = AndroidSQLiteDriver(),
            databaseClass = KongsiDatabase::class,
            databaseFactory = { KongsiDatabase_Impl() },
        )

    @Test
    fun payment_accounts_survive_the_upgrade_that_adds_bills() {
        // The schema assets exist only in the debug variant (feature/bill/impl/build.gradle.kts).
        assumeTrue("Room schemas are debug-only assets", BuildConfig.DEBUG)
        helper.createDatabase(1).apply {
            execSQL(
                "INSERT INTO payment_accounts (id, type, provider, number, holderName, isDefault, createdAt) " +
                    "VALUES (7, 'BANK', 'BCA', '1234567890', 'Ricky', 1, 42)",
            )
            close()
        }

        val upgraded = helper.runMigrationsAndValidate(2)

        assertEquals(
            "7|BCA|1234567890|1",
            upgraded.single("SELECT id || '|' || provider || '|' || number || '|' || isDefault FROM payment_accounts"),
        )
        assertEquals("0", upgraded.single("SELECT COUNT(*) FROM bills"))
    }

    private fun SQLiteConnection.single(sql: String): String =
        prepare(sql).use {
            check(it.step()) { "No row for $sql" }
            it.getText(0)
        }
}
