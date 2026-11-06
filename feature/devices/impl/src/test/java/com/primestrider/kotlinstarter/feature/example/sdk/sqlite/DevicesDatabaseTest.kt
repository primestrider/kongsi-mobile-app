package com.primestrider.kotlinstarter.feature.example.sdk.sqlite

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DevicesDatabaseTest {
    @Test
    fun transactions_live_in_devices_db() =
        runTest {
            val database =
                Room
                    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DevicesDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            try {
                assertEquals(DEVICES_DATABASE_NAME, "devices.db")
                database.transactionDao().insert(TransactionEntity(kind = TransactionKind.TOP_UP.name, amount = 100, createdAt = 1))
                assertEquals(
                    listOf(100L),
                    database
                        .transactionDao()
                        .latest()
                        .first()
                        .map { it.amount },
                )
            } finally {
                database.close()
            }
        }
}
