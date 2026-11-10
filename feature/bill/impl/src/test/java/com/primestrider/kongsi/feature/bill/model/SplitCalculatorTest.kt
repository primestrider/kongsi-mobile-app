package com.primestrider.kongsi.feature.bill.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SplitCalculatorTest {
    private val budi = Person(id = 1, name = "Budi")
    private val ani = Person(id = 2, name = "Ani")

    /** The plan's worked example: service 5%, then PB1 10% on subtotal + service. */
    private fun dinner(
        ochaServiceable: Boolean = true,
        ochaTaxable: Boolean = true,
        tax: BillCharge = BillCharge(13_860, rateBps = 1000),
    ) = Bill(
        people = listOf(budi, ani),
        items =
            listOf(
                BillItem(id = 10, name = "Nasi Goreng", totalMinor = 30_000, sharedBy = setOf(budi.id)),
                BillItem(id = 11, name = "Steak", totalMinor = 90_000, sharedBy = setOf(ani.id)),
                BillItem(
                    id = 12,
                    name = "Ocha",
                    totalMinor = 12_000,
                    sharedBy = setOf(budi.id, ani.id),
                    serviceable = ochaServiceable,
                    taxable = ochaTaxable,
                ),
            ),
        service = BillCharge(6_600, rateBps = 500),
        tax = tax,
    )

    private fun SplitResult.share(person: Person) = shares.single { it.personId == person.id }

    @Test
    fun worked_example_matches_the_plan() {
        val result = SplitCalculator.split(dinner())

        val budiShare = result.share(budi)
        assertEquals(36_000, budiShare.itemsMinor)
        assertEquals(1_800, budiShare.serviceMinor)
        assertEquals(3_780, budiShare.taxMinor)
        assertEquals(41_580, budiShare.totalMinor)
        assertEquals(110_880, result.share(ani).totalMinor)
        assertEquals(152_460, result.totalMinor)
        assertTrue(result.issues.isEmpty())
        assertTrue(result.isComplete)
    }

    @Test
    fun each_share_lists_the_persons_part_of_every_item_they_had() {
        val result = SplitCalculator.split(dinner())

        assertEquals(
            listOf(ItemPart(itemId = 10, sharers = 1, amountMinor = 30_000), ItemPart(itemId = 12, sharers = 2, amountMinor = 6_000)),
            result.share(budi).items,
        )
        assertEquals(0, result.share(budi).roundingMinor)
    }

    @Test
    fun item_parts_round_on_their_own_and_the_difference_is_reported() {
        // Five items of Rp10.001 shared by two: each part is Rp5.000,5 and shows as Rp5.001.
        val items = (1L..5L).map { BillItem(it, "Item $it", 10_001, sharedBy = setOf(budi.id, ani.id)) }
        val result = SplitCalculator.split(Bill(people = listOf(budi, ani), items = items))

        val share = result.share(budi)
        assertEquals(List(5) { 5_001L }, share.items.map { it.amountMinor })
        assertEquals(share.itemsMinor - 25_005, share.roundingMinor)
        assertEquals(-2, share.roundingMinor)
    }

    @Test
    fun shared_item_splits_equally() {
        val people = (1L..4L).map { Person(it, "P$it") }
        val bill = Bill(people = people, items = listOf(BillItem(1, "Pizza", 100_000, sharedBy = people.map { it.id }.toSet())))

        val result = SplitCalculator.split(bill)

        assertEquals(List(4) { 25_000L }, result.shares.map { it.totalMinor })
    }

    @Test
    fun remainder_goes_to_one_person_so_the_total_still_adds_up() {
        val people = (1L..3L).map { Person(it, "P$it") }
        val bill = Bill(people = people, items = listOf(BillItem(1, "Pizza", 100_000, sharedBy = people.map { it.id }.toSet())))

        val totals = SplitCalculator.split(bill).shares.map { it.totalMinor }

        assertEquals(listOf(33_334L, 33_333L, 33_333L), totals)
    }

    @Test
    fun item_without_service_moves_its_service_to_the_other_items() {
        val result = SplitCalculator.split(dinner(ochaServiceable = false))

        assertEquals(1_650, result.share(budi).serviceMinor)
        assertEquals(4_950, result.share(ani).serviceMinor)
        assertEquals(41_415, result.share(budi).totalMinor)
        assertEquals(111_045, result.share(ani).totalMinor)
        assertEquals(152_460, result.totalMinor)
    }

    @Test
    fun item_without_tax_moves_its_tax_to_the_other_items() {
        val result = SplitCalculator.split(dinner(ochaTaxable = false, tax = BillCharge(13_860)))

        assertEquals(3_465, result.share(budi).taxMinor)
        assertEquals(10_395, result.share(ani).taxMinor)
        assertEquals(41_265, result.share(budi).totalMinor)
        assertEquals(111_195, result.share(ani).totalMinor)
    }

    @Test
    fun service_on_an_item_without_tax_is_not_taxed() {
        val bill =
            Bill(
                people = listOf(budi, ani),
                items =
                    listOf(
                        BillItem(1, "Steak", 100_000, sharedBy = setOf(budi.id)),
                        BillItem(2, "Wine", 100_000, sharedBy = setOf(ani.id), taxable = false),
                    ),
                service = BillCharge(10_000),
                tax = BillCharge(10_500),
            )

        val result = SplitCalculator.split(bill)

        assertEquals(0, result.share(ani).taxMinor)
        assertEquals(105_000, result.share(ani).totalMinor)
        assertEquals(115_500, result.share(budi).totalMinor)
    }

    @Test
    fun tax_free_bill_with_no_tax_is_fine() {
        val bill =
            Bill(
                people = listOf(budi),
                items = listOf(BillItem(1, "Water", 5_000, sharedBy = setOf(budi.id), taxable = false)),
            )

        val result = SplitCalculator.split(bill)

        assertEquals(5_000, result.share(budi).totalMinor)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun tax_without_any_taxable_item_blocks_the_summary() {
        val bill =
            Bill(
                people = listOf(budi),
                items = listOf(BillItem(1, "Water", 5_000, sharedBy = setOf(budi.id), taxable = false)),
                tax = BillCharge(500),
            )

        val result = SplitCalculator.split(bill)

        assertEquals(listOf(SplitIssue.NoEligibleItem(ChargeType.TAX)), result.issues)
        assertFalse(result.isComplete)
    }

    @Test
    fun rate_that_does_not_match_the_printed_amount_is_a_warning() {
        // 10% of the taxable base is 12.600 once the Ocha is tax-free, but the receipt says 13.860.
        val result = SplitCalculator.split(dinner(ochaTaxable = false))

        assertEquals(
            listOf(SplitIssue.RateMismatch(ChargeType.TAX, printedMinor = 13_860, calculatedMinor = 12_600)),
            result.issues,
        )
        assertTrue(result.isComplete)
    }

    @Test
    fun rate_within_one_percent_is_not_a_warning() {
        val result = SplitCalculator.split(dinner(tax = BillCharge(13_900, rateBps = 1000)))

        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun bill_discount_is_spread_by_price() {
        val bill =
            Bill(
                people = listOf(budi, ani),
                items =
                    listOf(
                        BillItem(1, "A", 60_000, sharedBy = setOf(budi.id)),
                        BillItem(2, "B", 40_000, sharedBy = setOf(ani.id)),
                    ),
                discountMinor = 10_000,
            )

        val result = SplitCalculator.split(bill)

        assertEquals(6_000, result.share(budi).discountMinor)
        assertEquals(54_000, result.share(budi).totalMinor)
        assertEquals(36_000, result.share(ani).totalMinor)
        assertEquals(90_000, result.totalMinor)
    }

    @Test
    fun payer_gets_a_share_like_everyone_and_a_person_with_nothing_owes_zero() {
        val me = Person(id = 3, name = "Me")
        val bill =
            Bill(
                people = listOf(me, budi, ani),
                items =
                    listOf(
                        BillItem(1, "Coffee", 20_000, sharedBy = setOf(me.id)),
                        BillItem(2, "Tea", 10_000, sharedBy = setOf(budi.id)),
                    ),
            )

        val result = SplitCalculator.split(bill)

        assertEquals(listOf(me.id, budi.id, ani.id), result.shares.map { it.personId })
        assertEquals(20_000, result.share(me).totalMinor)
        assertEquals(0, result.share(ani).totalMinor)
    }

    @Test
    fun unassigned_item_blocks_the_summary() {
        val bill =
            Bill(
                people = listOf(budi),
                items =
                    listOf(
                        BillItem(1, "Tea", 10_000, sharedBy = setOf(budi.id)),
                        BillItem(2, "Cake", 25_000, sharedBy = emptySet()),
                    ),
            )

        val result = SplitCalculator.split(bill)

        assertEquals(listOf(SplitIssue.UnassignedItems(listOf(2L))), result.issues)
        assertFalse(result.isComplete)
        assertEquals(10_000, result.assignedMinor)
        assertEquals(35_000, result.totalMinor)
    }

    @Test
    fun receipt_total_that_does_not_match_is_a_warning() {
        val result = SplitCalculator.split(dinner().copy(receiptTotalMinor = 150_000))

        assertEquals(listOf(SplitIssue.ReceiptTotalMismatch(receiptMinor = 150_000, calculatedMinor = 152_460)), result.issues)
    }

    @Test
    fun rate_fills_service_from_serviceable_items_and_tax_from_items_plus_service() {
        val bill = dinner()

        assertEquals(6_600, SplitCalculator.chargeAmount(bill, ChargeType.SERVICE, rateBps = 500))
        assertEquals(13_860, SplitCalculator.chargeAmount(bill, ChargeType.TAX, rateBps = 1000))
    }

    @Test
    fun rate_ignores_items_the_charge_does_not_apply_to() {
        // Ocha without tax: 10% of Nasi Goreng and Steak plus their service (31.500 + 94.500).
        assertEquals(12_600, SplitCalculator.chargeAmount(dinner(ochaTaxable = false), ChargeType.TAX, rateBps = 1000))
        // Ocha without service: 5% of 120.000.
        assertEquals(6_000, SplitCalculator.chargeAmount(dinner(ochaServiceable = false), ChargeType.SERVICE, rateBps = 500))
    }

    @Test
    fun shares_always_add_up_to_the_bill_total() {
        val random = Random(seed = 42)
        repeat(2_000) { round ->
            val bill = randomBill(random)
            val result = SplitCalculator.split(bill)
            val expected = bill.items.sumOf { it.totalMinor } + bill.service.amountMinor + bill.tax.amountMinor - bill.discountMinor

            assertTrue("round $round: ${result.issues}", result.isComplete)
            assertEquals("round $round: total", expected, result.totalMinor)
            assertEquals("round $round: shares", expected, result.shares.sumOf { it.totalMinor })
            result.shares.forEach { share ->
                val parts = share.itemsMinor + share.serviceMinor + share.taxMinor - share.discountMinor
                assertEquals("round $round: parts of ${share.personId}", share.totalMinor, parts)
            }
            // Fair tax and service: someone who only had tax-free (service-free) items pays none of it.
            bill.people.forEach { person ->
                val theirs = bill.items.filter { person.id in it.sharedBy }
                val share = result.shares.single { it.personId == person.id }
                if (theirs.none { it.taxable }) assertEquals("round $round: tax", 0, share.taxMinor)
                if (theirs.none { it.serviceable }) assertEquals("round $round: service", 0, share.serviceMinor)
            }
        }
    }

    private fun randomBill(random: Random): Bill {
        val people = (1L..random.nextLong(1, 7)).map { Person(it, "P$it") }
        val items =
            (1L..random.nextLong(1, 11)).map { id ->
                val sharers = people.filter { random.nextBoolean() }.map { it.id }.ifEmpty { listOf(people.random(random).id) }
                BillItem(
                    id = id,
                    name = "Item $id",
                    totalMinor = random.nextLong(1, 500_001),
                    sharedBy = sharers.toSet(),
                    serviceable = random.nextBoolean(),
                    taxable = random.nextBoolean(),
                )
            }
        val subtotal = items.sumOf { it.totalMinor }
        val service = if (items.any { it.serviceable }) random.nextLong(0, subtotal / 10 + 1) else 0
        val tax = if (items.any { it.taxable }) random.nextLong(0, subtotal / 5 + 1) else 0
        return Bill(
            people = people,
            items = items,
            service = BillCharge(service),
            tax = BillCharge(tax),
            discountMinor = random.nextLong(0, subtotal / 2 + 1),
        )
    }
}
