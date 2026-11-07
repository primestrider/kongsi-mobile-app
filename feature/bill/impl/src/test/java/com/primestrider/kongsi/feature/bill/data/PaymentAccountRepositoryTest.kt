package com.primestrider.kongsi.feature.bill.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PaymentAccountRepositoryTest {
    private lateinit var database: KongsiDatabase
    private lateinit var repository: PaymentAccountRepository
    private var now = 0L

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KongsiDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = DefaultPaymentAccountRepository(database.paymentAccountDao()) { ++now }
    }

    @After
    fun tearDown() = database.close()

    private fun account(
        provider: String,
        type: PaymentAccountType = PaymentAccountType.BANK,
    ) = PaymentAccount(type = type, provider = provider, number = "1234567890", holderName = "Ricky", isDefault = false)

    @Test
    fun saved_accounts_come_back_in_the_order_they_were_added() =
        runTest {
            repository.save(account("BCA"))
            repository.save(account("GoPay", PaymentAccountType.EWALLET))
            val stored = repository.accounts.first()
            assertEquals(listOf("BCA", "GoPay"), stored.map { it.provider })
            assertEquals(PaymentAccountType.EWALLET, stored[1].type)
        }

    @Test
    fun editing_replaces_the_account_and_keeps_its_place() =
        runTest {
            val first = repository.save(account("BCA"))
            repository.save(account("BRI"))
            val stored = requireNotNull(repository.get(first))
            assertEquals(first, repository.save(stored.copy(number = "999999", isDefault = true)))
            val all = repository.accounts.first()
            assertEquals(listOf("BCA", "BRI"), all.map { it.provider })
            assertEquals("999999", all[0].number)
            assertEquals(true, all[0].isDefault)
        }

    @Test
    fun delete_returns_the_account_and_saving_it_again_restores_its_place() =
        runTest {
            val first = repository.save(account("BCA"))
            repository.save(account("BRI"))
            val removed = requireNotNull(repository.delete(first))
            assertEquals(listOf("BRI"), repository.accounts.first().map { it.provider })
            repository.save(removed)
            assertEquals(listOf("BCA", "BRI"), repository.accounts.first().map { it.provider })
        }

    @Test
    fun deleting_a_missing_account_returns_null() =
        runTest {
            assertNull(repository.delete(42))
        }
}
