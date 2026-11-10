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

    @Test
    fun bills_survive_the_upgrade_that_adds_frequent_people() {
        assumeTrue("Room schemas are debug-only assets", BuildConfig.DEBUG)
        helper.createDatabase(2).apply {
            execSQL(
                "INSERT INTO bills (id, title, createdAt, currency, payerPersonId, serviceMinor, serviceRateBps, taxMinor, " +
                    "taxRateBps, discountMinor, receiptTotalMinor) VALUES (3, 'Sushi', 1, 'IDR', NULL, 0, NULL, 0, NULL, 0, NULL)",
            )
            close()
        }

        val upgraded = helper.runMigrationsAndValidate(3)

        assertEquals("Sushi", upgraded.single("SELECT title FROM bills"))
        assertEquals("0", upgraded.single("SELECT COUNT(*) FROM frequent_people"))
    }

    @Test
    fun bills_survive_the_upgrade_that_adds_their_payment_accounts() {
        assumeTrue("Room schemas are debug-only assets", BuildConfig.DEBUG)
        helper.createDatabase(3).apply {
            execSQL(
                "INSERT INTO bills (id, title, createdAt, currency, payerPersonId, serviceMinor, serviceRateBps, taxMinor, " +
                    "taxRateBps, discountMinor, receiptTotalMinor) VALUES (3, 'Sushi', 1, 'IDR', NULL, 0, NULL, 0, NULL, 0, NULL)",
            )
            close()
        }

        val upgraded = helper.runMigrationsAndValidate(4)

        // Existing bills have not chosen yet, so the default accounts apply to them.
        assertEquals("0", upgraded.single("SELECT accountsChosen FROM bills"))
        assertEquals("0", upgraded.single("SELECT COUNT(*) FROM bill_payment_accounts"))
    }

    @Test
    fun people_survive_the_upgrade_that_adds_who_paid() {
        assumeTrue("Room schemas are debug-only assets", BuildConfig.DEBUG)
        helper.createDatabase(4).apply {
            execSQL(
                "INSERT INTO bills (id, title, createdAt, currency, payerPersonId, serviceMinor, serviceRateBps, taxMinor, " +
                    "taxRateBps, discountMinor, receiptTotalMinor, accountsChosen) VALUES (3, 'Sushi', 1, 'IDR', NULL, 0, NULL, 0, NULL, 0, NULL, 0)",
            )
            execSQL("INSERT INTO bill_people (id, billId, name, phone, position) VALUES (5, 3, 'Budi', NULL, 0)")
            close()
        }

        val upgraded = helper.runMigrationsAndValidate(5)

        // Nobody has paid yet on bills from before.
        assertEquals("Budi", upgraded.single("SELECT name FROM bill_people"))
        assertEquals("0", upgraded.single("SELECT paid FROM bill_people"))
    }

    private fun SQLiteConnection.single(sql: String): String =
        prepare(sql).use {
            check(it.step()) { "No row for $sql" }
            it.getText(0)
        }
}
