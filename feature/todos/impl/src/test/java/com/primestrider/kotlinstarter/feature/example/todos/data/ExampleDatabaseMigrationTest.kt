package com.primestrider.kotlinstarter.feature.example.sdk.sqlite

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.primestrider.kotlinstarter.feature.example.todos.data.ExampleDatabase
import com.primestrider.kotlinstarter.feature.example.todos.data.ExampleDatabase_Impl
import com.primestrider.kotlinstarter.feature.example.todos.data.MIGRATION_1_2
import com.primestrider.kotlinstarter.feature.example.todos.data.MIGRATION_2_3
import com.primestrider.kotlinstarter.feature.todos.impl.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Upgrades keep todos. Reads the exported schemas in `feature/todos/impl/schemas` (debug assets). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ExampleDatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = File.createTempFile("migration-test", ".db"),
            driver = AndroidSQLiteDriver(),
            databaseClass = ExampleDatabase::class,
            databaseFactory = { ExampleDatabase_Impl() },
        )

    @Test
    fun todos_survive_the_upgrade_from_v1_to_v2() {
        // The schema assets exist only in the debug variant (feature/todos/impl/build.gradle.kts).
        assumeTrue("Room schemas are debug-only assets", BuildConfig.DEBUG)
        helper.createDatabase(1).apply {
            execSQL("INSERT INTO todos (id, title, done, createdAt) VALUES ('a', 'Keep me', 1, 42)")
            close()
        }

        val upgraded = helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2))

        assertEquals("a|Keep me|1|42", upgraded.single("SELECT id || '|' || title || '|' || done || '|' || createdAt FROM todos"))
        assertEquals("0", upgraded.single("SELECT COUNT(*) FROM transactions"))
    }

    /** The devices demo's table moved to its own database; existing installs keep their todos and lose that table. */
    @Test
    fun migrate_2_to_3_keeps_todos_and_drops_transactions() {
        assumeTrue("Room schemas are debug-only assets", BuildConfig.DEBUG)
        helper.createDatabase(2).apply {
            execSQL("INSERT INTO todos (id, title, done, createdAt) VALUES ('a', 'Keep me', 0, 7)")
            execSQL("INSERT INTO transactions (kind, amount, createdAt) VALUES ('IN', 100, 1)")
            close()
        }

        val upgraded = helper.runMigrationsAndValidate(3, listOf(MIGRATION_2_3))

        assertEquals("a|Keep me|0|7", upgraded.single("SELECT id || '|' || title || '|' || done || '|' || createdAt FROM todos"))
        assertEquals("0", upgraded.single("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'transactions'"))
    }

    private fun SQLiteConnection.single(sql: String): String =
        prepare(sql).use {
            check(it.step()) { "No row for $sql" }
            it.getText(0)
        }
}
