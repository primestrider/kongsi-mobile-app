package com.primestrider.kongsi.feature.bill.share

import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import com.primestrider.kongsi.feature.bill.model.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class BillMessageBuilderTest {
    private val me = 1L
    private val budi = 2L
    private val ani = 3L
    private val citra = 4L

    // The plan's worked example, paid by Ricky: service 5%, then PB1 10% on subtotal + service.
    private val dinner =
        Bill(
            id = 7,
            title = "Makan Malam Sushi",
            // 6 Nov 2026, 12:00 UTC.
            createdAt = 1_793_966_400_000,
            payerPersonId = me,
            people = listOf(Person(me, "Ricky"), Person(budi, "Budi"), Person(ani, "Ani")),
            items =
                listOf(
                    BillItem(10, "Nasi Goreng", 30_000, sharedBy = setOf(budi)),
                    BillItem(11, "Steak", 90_000, sharedBy = setOf(ani)),
                    BillItem(12, "Ocha", 12_000, sharedBy = setOf(budi, ani)),
                ),
            service = BillCharge(6_600, 500),
            tax = BillCharge(13_860, 1_000),
        )

    private val bca = PaymentAccount(1, PaymentAccountType.BANK, "BCA", "1234567890", "Ricky", isDefault = true)
    private val gopay = PaymentAccount(2, PaymentAccountType.EWALLET, "GoPay", "081234567890", "Ricky", isDefault = false)

    private val english: Locale = Locale.US
    private val indonesian: Locale = Locale.forLanguageTag("id-ID")
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun message(
        locale: Locale,
        accounts: List<PaymentAccount> = listOf(bca, gopay),
        bill: Bill = dinner,
        personId: Long = budi,
    ) = BillMessageBuilder.forPerson(bill, personId, accounts, locale, utc)

    @Test
    fun indonesian_message_explains_the_total_and_lists_every_account() {
        assertEquals(
            """
            Halo Budi, ini bagianmu dari tagihan "Makan Malam Sushi" (6 Nov 2026):
            - Nasi Goreng: 30.000
            - Ocha (bareng Ani, ½): 6.000
            - Servis 5% dari pesananmu: 1.800
            - Pajak 10% dari pesananmu + servis: 3.780
            *Total: 41.580*

            Bisa transfer ke:
            BCA a.n. Ricky
            1234567890
            GoPay a.n. Ricky
            081234567890

            Dikirim dari Kongsi
            """.trimIndent(),
            message(indonesian),
        )
    }

    @Test
    fun english_message_explains_the_total_and_lists_every_account() {
        assertEquals(
            """
            Hi Budi, here's your part of the bill "Makan Malam Sushi" (Nov 6, 2026):
            - Nasi Goreng: 30,000
            - Ocha (shared with Ani, ½): 6,000
            - Service 5% of your order: 1,800
            - Tax 10% of your order + service: 3,780
            *Total: 41,580*

            You can transfer to:
            BCA (Ricky)
            1234567890
            GoPay (Ricky)
            081234567890

            Sent from Kongsi
            """.trimIndent(),
            message(english),
        )
    }

    @Test
    fun one_account_is_listed_on_its_own() {
        val text = message(indonesian, accounts = listOf(gopay))

        assertTrue(text.contains("Bisa transfer ke:\nGoPay a.n. Ricky\n081234567890\n\nDikirim dari Kongsi"))
        assertFalse(text.contains("BCA"))
    }

    @Test
    fun without_accounts_there_is_no_transfer_part() {
        assertEquals(
            """
            Hi Budi, here's your part of the bill "Makan Malam Sushi" (Nov 6, 2026):
            - Nasi Goreng: 30,000
            - Ocha (shared with Ani, ½): 6,000
            - Service 5% of your order: 1,800
            - Tax 10% of your order + service: 3,780
            *Total: 41,580*

            Sent from Kongsi
            """.trimIndent(),
            message(english, accounts = emptyList()),
        )
    }

    @Test
    fun account_numbers_stand_alone_on_their_line_so_they_copy_cleanly() {
        val lines = message(indonesian).lines()

        assertTrue("1234567890" in lines)
        assertTrue("081234567890" in lines)
    }

    @Test
    fun an_item_shared_by_three_names_the_others_and_the_fraction() {
        val bill =
            dinner.copy(
                people = dinner.people + Person(citra, "Citra"),
                items = dinner.items.map { if (it.id == 12L) it.copy(sharedBy = setOf(me, budi, citra), quantity = 3) else it },
            )

        val text = message(indonesian, bill = bill)

        assertTrue(text, text.contains("- 3× Ocha (bareng Ricky, Citra, ⅓): 4.000"))
    }

    @Test
    fun group_summary_lists_what_everyone_owes_in_indonesian() {
        assertEquals(
            """
            Ini pembagian tagihan "Makan Malam Sushi" (6 Nov 2026):
            - Budi: 41.580
            - Ani: 110.880
            *Total: 152.460*

            Bisa transfer ke:
            BCA a.n. Ricky
            1234567890

            Dikirim dari Kongsi
            """.trimIndent(),
            BillMessageBuilder.forGroup(dinner, listOf(bca), indonesian, utc),
        )
    }

    @Test
    fun group_summary_names_the_payer_last_when_they_had_something() {
        val bill = dinner.copy(title = "", items = dinner.items.map { if (it.id == 10L) it.copy(sharedBy = setOf(me)) else it })

        assertEquals(
            """
            Here's the split for the bill (Nov 6, 2026):
            - Budi: 6,930
            - Ani: 110,880
            - Ricky (paid the bill): 34,650
            *Total: 152,460*

            Sent from Kongsi
            """.trimIndent(),
            BillMessageBuilder.forGroup(bill, emptyList(), english, utc),
        )
    }

    @Test
    fun charges_without_a_rate_a_discount_and_no_title_are_worded_plainly() {
        // Budi's items are 36.000 of 132.000, so his part of each charge is 36/132.
        val bill = dinner.copy(title = "", service = BillCharge(), tax = BillCharge(13_200), discountMinor = 6_600)

        val text = message(english, bill = bill)

        assertTrue(text, text.startsWith("Hi Budi, here's your part of the bill (Nov 6, 2026):\n"))
        assertTrue(text, text.contains("- Tax on your order: 3,600\n- Discount, by what you ordered: −1,800\n*Total: 37,800*"))
    }
}
