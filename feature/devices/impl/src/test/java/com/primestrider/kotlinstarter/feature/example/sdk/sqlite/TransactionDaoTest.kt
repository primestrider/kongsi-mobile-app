package com.primestrider.kotlinstarter.feature.example.sdk.sqlite

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TransactionDaoTest {
    private lateinit var database: DevicesDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DevicesDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.transactionDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun balance_sums_signed_amounts() =
        runTest {
            assertEquals(0L, dao.balance().first())
            dao.insert(TransactionEntity(kind = "TOP_UP", amount = 100_000, createdAt = 1))
            dao.insert(TransactionEntity(kind = "PAYMENT", amount = -30_000, createdAt = 2))
            assertEquals(70_000L, dao.balance().first())
        }

    @Test
    fun latest_orders_by_time_then_id() =
        runTest {
            dao.insert(TransactionEntity(kind = "TOP_UP", amount = 1, createdAt = 5))
            dao.insert(TransactionEntity(kind = "TOP_UP", amount = 2, createdAt = 9))
            dao.insert(TransactionEntity(kind = "TOP_UP", amount = 3, createdAt = 5))
            assertEquals(listOf(2L, 3L, 1L), dao.latest().first().map { it.amount })
        }

    @Test
    fun latest_caps_at_twenty() =
        runTest {
            repeat(25) { dao.insert(TransactionEntity(kind = "TOP_UP", amount = 1, createdAt = it.toLong())) }
            assertEquals(20, dao.latest().first().size)
        }

    @Test
    fun clear_empties_the_table() =
        runTest {
            dao.insert(TransactionEntity(kind = "TOP_UP", amount = 1, createdAt = 1))
            dao.clear()
            assertTrue(dao.latest().first().isEmpty())
        }
}
