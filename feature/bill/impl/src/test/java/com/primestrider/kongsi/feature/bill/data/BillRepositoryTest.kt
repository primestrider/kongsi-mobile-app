package com.primestrider.kongsi.feature.bill.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import com.primestrider.kongsi.feature.bill.model.Person
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
class BillRepositoryTest {
    private lateinit var database: KongsiDatabase
    private lateinit var repository: BillRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KongsiDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = DefaultBillRepository(database.billDao()) { ++now }
    }

    @After
    fun tearDown() = database.close()

    /** A saved bill with two people, then its items shared between them. */
    private suspend fun dinner(): Bill {
        val id =
            repository.save(
                Bill(
                    title = "Makan Malam",
                    people = listOf(Person(id = 0, name = "Budi", phone = "6281234567890"), Person(id = 0, name = "Ani")),
                    items = emptyList(),
                ),
            )
        val saved = requireNotNull(repository.get(id))
        val (budi, ani) = saved.people
        repository.save(
            saved.copy(
                items =
                    listOf(
                        BillItem(id = 0, name = "Nasi Goreng", totalMinor = 30_000, sharedBy = setOf(budi.id)),
                        BillItem(id = 0, name = "Steak", totalMinor = 90_000, sharedBy = setOf(ani.id)),
                        BillItem(
                            id = 0,
                            name = "Ocha",
                            totalMinor = 12_000,
                            quantity = 2,
                            sharedBy = setOf(budi.id, ani.id),
                            serviceable = false,
                        ),
                    ),
                service = BillCharge(6_600, rateBps = 500),
                tax = BillCharge(13_860),
                discountMinor = 1_000,
                receiptTotalMinor = 151_460,
                payerPersonId = budi.id,
            ),
        )
        return requireNotNull(repository.get(id))
    }

    @Test
    fun saved_bill_comes_back_with_items_in_order_people_and_shares() =
        runTest {
            val bill = dinner()

            assertEquals("Makan Malam", bill.title)
            assertEquals(1_001L, bill.createdAt)
            assertEquals("IDR", bill.currency)
            assertEquals(listOf("Budi", "Ani"), bill.people.map { it.name })
            assertEquals("6281234567890", bill.people[0].phone)
            assertEquals(listOf("Nasi Goreng", "Steak", "Ocha"), bill.items.map { it.name })
            val (budi, ani) = bill.people
            assertEquals(setOf(budi.id, ani.id), bill.items[2].sharedBy)
            assertEquals(2, bill.items[2].quantity)
            assertEquals(false, bill.items[2].serviceable)
            assertEquals(true, bill.items[2].taxable)
            assertEquals(BillCharge(6_600, rateBps = 500), bill.service)
            assertEquals(BillCharge(13_860), bill.tax)
            assertEquals(1_000L, bill.discountMinor)
            assertEquals(151_460L, bill.receiptTotalMinor)
            assertEquals(budi.id, bill.payerPersonId)
        }

    @Test
    fun saving_again_replaces_items_people_and_shares() =
        runTest {
            val bill = dinner()
            val (budi, _) = bill.people
            val steak = bill.items[1]

            repository.save(
                bill.copy(
                    people = listOf(budi),
                    items = listOf(steak.copy(name = "Wagyu", sharedBy = setOf(budi.id)), bill.items[0]),
                ),
            )

            val edited = requireNotNull(repository.get(bill.id))
            assertEquals(listOf("Budi"), edited.people.map { it.name })
            assertEquals(listOf("Wagyu", "Nasi Goreng"), edited.items.map { it.name })
            assertEquals(steak.id, edited.items[0].id)
            assertEquals(setOf(budi.id), edited.items[0].sharedBy)
            assertEquals(1_001L, edited.createdAt)
        }

    @Test
    fun deleting_a_bill_removes_its_items_people_and_shares() =
        runTest {
            val bill = dinner()

            repository.delete(bill.id)

            assertNull(repository.get(bill.id))
            val counts =
                listOf("bills", "bill_items", "bill_people", "item_shares").map { table ->
                    database.query("SELECT COUNT(*) FROM $table", null).use {
                        it.moveToFirst()
                        it.getInt(0)
                    }
                }
            assertEquals(listOf(0, 0, 0, 0), counts)
        }

    @Test
    fun a_deleted_bill_can_be_restored_as_it_was() =
        runTest {
            val bill = dinner()

            val removed = requireNotNull(repository.delete(bill.id))
            repository.save(removed)

            assertEquals(bill, repository.get(bill.id))
        }

    private suspend fun account(provider: String): Long =
        DefaultPaymentAccountRepository(database.paymentAccountDao())
            .save(
                PaymentAccount(
                    type = PaymentAccountType.BANK,
                    provider = provider,
                    number = "1234567890",
                    holderName = "Ricky",
                    isDefault = true,
                ),
            )

    @Test
    fun chosen_payment_accounts_come_back_and_not_chosen_stays_null() =
        runTest {
            val bill = dinner()
            assertNull(bill.paymentAccountIds)
            val bca = account("BCA")
            account("BRI")

            repository.save(bill.copy(paymentAccountIds = setOf(bca)))
            assertEquals(setOf(bca), repository.get(bill.id)?.paymentAccountIds)

            // Choosing none is a choice too: it is not reset to the defaults.
            repository.save(bill.copy(paymentAccountIds = emptySet()))
            assertEquals(emptySet<Long>(), repository.get(bill.id)?.paymentAccountIds)
        }

    @Test
    fun deleting_an_account_removes_it_from_bills_and_a_stale_choice_still_saves() =
        runTest {
            val bill = dinner()
            val bca = account("BCA")
            val bri = account("BRI")
            repository.save(bill.copy(paymentAccountIds = setOf(bca, bri)))

            database.paymentAccountDao().delete(bca)
            assertEquals(setOf(bri), repository.get(bill.id)?.paymentAccountIds)

            // A screen that still holds the old choice saves without failing; the deleted account is left out.
            repository.save(bill.copy(paymentAccountIds = setOf(bca, bri)))
            assertEquals(setOf(bri), repository.get(bill.id)?.paymentAccountIds)
        }

    @Test
    fun bills_list_newest_first_and_update_after_changes() =
        runTest {
            val older = repository.save(Bill(title = "Kopi", people = emptyList(), items = emptyList()))
            val newer = repository.save(Bill(title = "Sushi", people = emptyList(), items = emptyList()))

            assertEquals(listOf(newer, older), repository.bills.first().map { it.id })
            repository.delete(newer)
            assertEquals(listOf("Kopi"), repository.bills.first().map { it.title })
            assertEquals("Kopi", repository.observe(older).first()?.title)
        }
}
