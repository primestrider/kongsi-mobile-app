package com.primestrider.kongsi.feature.bill.model

import java.math.BigInteger
import kotlin.math.abs

/** What one person owes, in minor units. The parts always add up: items + service + tax − discount = total. */
data class PersonShare(
    val personId: Long,
    val itemsMinor: Long,
    val serviceMinor: Long,
    val taxMinor: Long,
    val discountMinor: Long,
    val totalMinor: Long,
)

enum class ChargeType { SERVICE, TAX, DISCOUNT }

/** Something the Review, Assign or Summary step tells the user. [blocking] issues keep them off the Summary step. */
sealed interface SplitIssue {
    val blocking: Boolean

    /** Items nobody had yet ("2 items not assigned yet"). */
    data class UnassignedItems(
        val itemIds: List<Long>,
    ) : SplitIssue {
        override val blocking = true
    }

    /** A charge above zero with no item to spread it over (every item has it turned off). */
    data class NoEligibleItem(
        val charge: ChargeType,
    ) : SplitIssue {
        override val blocking = true
    }

    /** The charge's rate times its base differs from the printed amount by more than 1%. */
    data class RateMismatch(
        val charge: ChargeType,
        val printedMinor: Long,
        val calculatedMinor: Long,
    ) : SplitIssue {
        override val blocking = false
    }

    /** Items and charges do not add up to the total printed on the receipt. */
    data class ReceiptTotalMismatch(
        val receiptMinor: Long,
        val calculatedMinor: Long,
    ) : SplitIssue {
        override val blocking = false
    }
}

/**
 * The split of a bill. [shares] has one entry per person, in the bill's order. [totalMinor] is the bill total (items +
 * service + tax − discount). [assignedMinor] is the part already split between people; when the result [isComplete],
 * it equals [totalMinor] and so does the sum of the shares.
 */
data class SplitResult(
    val shares: List<PersonShare>,
    val totalMinor: Long,
    val assignedMinor: Long,
    val issues: List<SplitIssue>,
) {
    val isComplete: Boolean get() = issues.none { it.blocking }
}

/**
 * Splits a bill following the plan's Split Rules (docs/superpowers/plans/2026-11-06-kongsi-split-bill.md):
 * - each person pays an equal part of every item they had;
 * - service is spread over serviceable items by price;
 * - tax is spread over taxable items by price plus their service (Indonesian PB1: tax on subtotal + service);
 * - a bill-level discount is spread over all items by price.
 *
 * Every intermediate value is an exact fraction. Rounding happens once: person totals by the largest-remainder method,
 * so they add up to the bill total exactly, then each person's parts the same way, so they add up to that person's total.
 */
object SplitCalculator {
    fun split(bill: Bill): SplitResult {
        val issues = mutableListOf<SplitIssue>()
        val prices = bill.items.associate { it.id to Fraction.of(it.totalMinor) }

        val serviceBase = bill.items.filter { it.serviceable }.sumFractions { prices.getValue(it.id) }
        val service =
            spread(bill.items, bill.service.amountMinor, serviceBase, ChargeType.SERVICE, issues) { item ->
                if (item.serviceable) prices.getValue(item.id) else null
            }

        val taxBases = bill.items.associate { it.id to prices.getValue(it.id) + service.getValue(it.id) }
        val taxBase = bill.items.filter { it.taxable }.sumFractions { taxBases.getValue(it.id) }
        val tax =
            spread(bill.items, bill.tax.amountMinor, taxBase, ChargeType.TAX, issues) { item ->
                if (item.taxable) taxBases.getValue(item.id) else null
            }

        val discountBase = bill.items.sumFractions { prices.getValue(it.id) }
        val discount =
            spread(bill.items, bill.discountMinor, discountBase, ChargeType.DISCOUNT, issues) { item ->
                prices.getValue(item.id)
            }

        checkRate(bill.service, serviceBase, ChargeType.SERVICE, issues)
        checkRate(bill.tax, taxBase, ChargeType.TAX, issues)

        val knownPeople = bill.people.map { it.id }.toSet()
        val unassigned = bill.items.filter { item -> item.sharedBy.none { it in knownPeople } }.map { it.id }
        if (unassigned.isNotEmpty()) issues.add(0, SplitIssue.UnassignedItems(unassigned))

        val totalMinor = bill.items.sumOf { it.totalMinor } + bill.service.amountMinor + bill.tax.amountMinor - bill.discountMinor
        bill.receiptTotalMinor?.let { receipt ->
            if (receipt != totalMinor) issues += SplitIssue.ReceiptTotalMismatch(receipt, totalMinor)
        }

        // Exact parts per person: each item's price and charges divided equally between the people who had it.
        val parts = bill.people.associate { it.id to Parts() }
        bill.items.forEach { item ->
            val sharers = item.sharedBy.filter { it in knownPeople }
            if (sharers.isEmpty()) return@forEach
            val n = sharers.size.toLong()
            sharers.forEach { personId ->
                parts.getValue(personId).add(
                    items = prices.getValue(item.id) / n,
                    service = service.getValue(item.id) / n,
                    tax = tax.getValue(item.id) / n,
                    discount = discount.getValue(item.id) / n,
                )
            }
        }

        val exactTotals = bill.people.map { parts.getValue(it.id).total }
        val totals = largestRemainder(exactTotals, target = exactTotals.sumFractions { it }.roundHalfUp())
        val shares =
            bill.people.mapIndexed { index, person ->
                val p = parts.getValue(person.id)
                // Discount enters negatively, so all four parts round together towards the person's total.
                val (items, serviceMinor, taxMinor, minusDiscount) =
                    largestRemainder(listOf(p.items, p.service, p.tax, -p.discount), target = totals[index])
                PersonShare(person.id, items, serviceMinor, taxMinor, -minusDiscount, totals[index])
            }
        return SplitResult(shares, totalMinor, assignedMinor = totals.sum(), issues = issues)
    }

    /**
     * [amountMinor] spread over the items [weight] returns a weight for, in proportion to it; other items get zero.
     * With nothing to spread over, the charge is reported and spread over no one.
     */
    private fun spread(
        items: List<BillItem>,
        amountMinor: Long,
        base: Fraction,
        type: ChargeType,
        issues: MutableList<SplitIssue>,
        weight: (BillItem) -> Fraction?,
    ): Map<Long, Fraction> {
        if (amountMinor != 0L && base.isZero) issues += SplitIssue.NoEligibleItem(type)
        return items.associate { item ->
            val w = weight(item)
            item.id to if (w == null || base.isZero) Fraction.ZERO else Fraction.of(amountMinor) * w / base
        }
    }

    private fun checkRate(
        charge: BillCharge,
        base: Fraction,
        type: ChargeType,
        issues: MutableList<SplitIssue>,
    ) {
        val rate = charge.rateBps ?: return
        val calculated = (base * Fraction.of(rate.toLong()) / Fraction.of(BPS_PER_UNIT)).roundHalfUp()
        // More than 1% of the printed amount apart: |calculated − printed| × 100 > printed.
        if (abs(calculated - charge.amountMinor) * 100 > charge.amountMinor) {
            issues += SplitIssue.RateMismatch(type, charge.amountMinor, calculated)
        }
    }

    /**
     * Rounds [values] to whole minor units adding up to [target]: each is floored, then the units still missing go
     * one each to the values with the largest remainders (ties to the earlier value).
     */
    private fun largestRemainder(
        values: List<Fraction>,
        target: Long,
    ): List<Long> {
        val floors = values.map { it.floor() }
        val missing = (target - floors.sum()).toInt()
        val bonus =
            values.indices
                .sortedWith(compareByDescending<Int> { values[it] - Fraction.of(floors[it]) }.thenBy { it })
                .take(missing.coerceIn(0, values.size))
                .toSet()
        return floors.mapIndexed { index, floor -> if (index in bonus) floor + 1 else floor }
    }

    private const val BPS_PER_UNIT = 10_000L
}

/** One person's exact parts before rounding. */
private class Parts {
    var items = Fraction.ZERO
    var service = Fraction.ZERO
    var tax = Fraction.ZERO
    var discount = Fraction.ZERO
    val total: Fraction get() = items + service + tax - discount

    fun add(
        items: Fraction,
        service: Fraction,
        tax: Fraction,
        discount: Fraction,
    ) {
        this.items += items
        this.service += service
        this.tax += tax
        this.discount += discount
    }
}

/** An exact rational number, so splitting Rp100.000 three ways stays exactly 100000/3 until the final rounding. */
internal class Fraction private constructor(
    private val num: BigInteger,
    private val den: BigInteger,
) : Comparable<Fraction> {
    val isZero: Boolean get() = num.signum() == 0

    operator fun plus(other: Fraction) = of(num * other.den + other.num * den, den * other.den)

    operator fun minus(other: Fraction) = of(num * other.den - other.num * den, den * other.den)

    operator fun times(other: Fraction) = of(num * other.num, den * other.den)

    operator fun div(other: Fraction) = of(num * other.den, den * other.num)

    operator fun div(divisor: Long) = of(num, den * BigInteger.valueOf(divisor))

    operator fun unaryMinus() = of(-num, den)

    /** The largest whole number not above this one. */
    fun floor(): Long {
        val (q, r) = num.divideAndRemainder(den)
        return (if (r.signum() < 0) q - BigInteger.ONE else q).toLong()
    }

    fun roundHalfUp(): Long = (this + of(BigInteger.ONE, BigInteger.valueOf(2))).floor()

    override fun compareTo(other: Fraction): Int = (num * other.den).compareTo(other.num * den)

    override fun equals(other: Any?): Boolean = other is Fraction && num == other.num && den == other.den

    override fun hashCode(): Int = 31 * num.hashCode() + den.hashCode()

    override fun toString(): String = "$num/$den"

    companion object {
        val ZERO = of(0)

        fun of(value: Long) = Fraction(BigInteger.valueOf(value), BigInteger.ONE)

        /** Normalised: lowest terms, positive denominator. */
        fun of(
            num: BigInteger,
            den: BigInteger,
        ): Fraction {
            require(den.signum() != 0) { "Division by zero" }
            val gcd = num.gcd(den).let { if (it.signum() == 0) BigInteger.ONE else it }
            val sign = if (den.signum() < 0) -BigInteger.ONE else BigInteger.ONE
            return Fraction(num / gcd * sign, den / gcd * sign)
        }
    }
}

private fun <T> Iterable<T>.sumFractions(selector: (T) -> Fraction): Fraction = fold(Fraction.ZERO) { acc, it -> acc + selector(it) }
