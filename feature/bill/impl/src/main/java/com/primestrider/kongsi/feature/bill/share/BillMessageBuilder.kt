package com.primestrider.kongsi.feature.bill.share

import com.primestrider.kongsi.core.util.formatDate
import com.primestrider.kongsi.core.util.formatMoney
import com.primestrider.kongsi.core.util.formatPercent
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BreakdownLine
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
import com.primestrider.kongsi.feature.bill.model.breakdownOf
import com.primestrider.kongsi.feature.bill.model.fractionText
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * The WhatsApp message asking someone for their part of a bill: the same breakdown as "How was this calculated?" on the
 * Summary (UX Principles, point 9), their total in bold, then the payer's accounts with each number on a line of its own
 * so it can be long-pressed and copied. Pure Kotlin with its own en/id wording, so the text is the same wherever it is
 * built and can be checked word for word in tests.
 */
object BillMessageBuilder {
    /**
     * The message for [personId] on [bill], in [locale] (Indonesian for `id`/`in`, otherwise English), listing
     * [accounts] in the order given. The bill's date is read in [timeZone].
     */
    fun forPerson(
        bill: Bill,
        personId: Long,
        accounts: List<PaymentAccount>,
        locale: Locale,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String {
        val words = if (locale.language == "id" || locale.language == "in") Words.INDONESIAN else Words.ENGLISH
        val money = { minor: Long -> formatMoney(minor, bill.currency, locale) }
        val share = SplitCalculator.split(bill).shares.first { it.personId == personId }
        val person = bill.people.first { it.id == personId }
        val names =
            bill.people.associate { p ->
                p.id to if (p.id == bill.payerPersonId) p.name.ifBlank { words.me } else p.name
            }

        return buildString {
            appendLine(words.greeting(person.name, bill.title, formatDate(bill.createdAt, locale, timeZone)))
            breakdownOf(bill, share).forEach { line ->
                val amount =
                    when (line) {
                        is BreakdownLine.Discount -> "−" + money(line.amountMinor)
                        is BreakdownLine.Rounding ->
                            if (line.amountMinor <
                                0
                            ) {
                                "−" + money(abs(line.amountMinor))
                            } else {
                                money(line.amountMinor)
                            }
                        else -> money(line.amountMinor)
                    }
                appendLine("- ${words.label(line, locale, names)}: $amount")
            }
            appendLine("*${words.total}: ${money(share.totalMinor)}*")
            if (accounts.isNotEmpty()) {
                appendLine()
                appendLine(words.transferTo)
                accounts.forEach { account ->
                    appendLine(words.account(account.provider, account.holderName))
                    appendLine(account.number)
                }
            }
            appendLine()
            append(words.sentFrom)
        }
    }

    /** The message's wording in one language. */
    private enum class Words(
        val me: String,
        val total: String,
        val transferTo: String,
        val sentFrom: String,
        private val greetingTitled: String,
        private val greetingUntitled: String,
        private val shared: String,
        private val serviceRate: String,
        private val service: String,
        private val taxRateService: String,
        private val taxRate: String,
        private val taxService: String,
        private val tax: String,
        private val discount: String,
        private val rounding: String,
        private val accountLine: String,
    ) {
        ENGLISH(
            me = "me",
            total = "Total",
            transferTo = "You can transfer to:",
            sentFrom = "Sent from Kongsi",
            greetingTitled = "Hi %1\$s, here's your part of the bill \"%2\$s\" (%3\$s):",
            greetingUntitled = "Hi %1\$s, here's your part of the bill (%3\$s):",
            shared = "%1\$s (shared with %2\$s, %3\$s)",
            serviceRate = "Service %1\$s of your order",
            service = "Service on your order",
            taxRateService = "Tax %1\$s of your order + service",
            taxRate = "Tax %1\$s of your order",
            taxService = "Tax on your order + service",
            tax = "Tax on your order",
            discount = "Discount, by what you ordered",
            rounding = "Rounding",
            accountLine = "%1\$s (%2\$s)",
        ),
        INDONESIAN(
            me = "aku",
            total = "Total",
            transferTo = "Bisa transfer ke:",
            sentFrom = "Dikirim dari Kongsi",
            greetingTitled = "Halo %1\$s, ini bagianmu dari tagihan \"%2\$s\" (%3\$s):",
            greetingUntitled = "Halo %1\$s, ini bagianmu dari tagihan (%3\$s):",
            shared = "%1\$s (bareng %2\$s, %3\$s)",
            serviceRate = "Servis %1\$s dari pesananmu",
            service = "Servis untuk pesananmu",
            taxRateService = "Pajak %1\$s dari pesananmu + servis",
            taxRate = "Pajak %1\$s dari pesananmu",
            taxService = "Pajak untuk pesananmu + servis",
            tax = "Pajak untuk pesananmu",
            discount = "Diskon, sesuai pesananmu",
            rounding = "Pembulatan",
            accountLine = "%1\$s a.n. %2\$s",
        ),
        ;

        fun greeting(
            name: String,
            title: String,
            date: String,
        ): String = (if (title.isBlank()) greetingUntitled else greetingTitled).format(name, title.trim(), date)

        fun account(
            provider: String,
            holder: String,
        ): String = accountLine.format(provider, holder)

        fun label(
            line: BreakdownLine,
            locale: Locale,
            names: Map<Long, String>,
        ): String {
            val rate = { bps: Int -> formatPercent(bps / BPS_PER_UNIT, locale) }
            return when (line) {
                is BreakdownLine.Item -> {
                    val name = if (line.quantity > 1) "${line.quantity}× ${line.name}" else line.name
                    if (line.sharedWith.isEmpty()) {
                        name
                    } else {
                        shared.format(name, line.sharedWith.mapNotNull { names[it] }.joinToString(", "), fractionText(line.sharers))
                    }
                }
                is BreakdownLine.Service -> line.rateBps?.let { serviceRate.format(rate(it)) } ?: service
                is BreakdownLine.Tax ->
                    when {
                        line.rateBps != null && line.onService -> taxRateService.format(rate(line.rateBps))
                        line.rateBps != null -> taxRate.format(rate(line.rateBps))
                        line.onService -> taxService
                        else -> tax
                    }
                is BreakdownLine.Discount -> discount
                is BreakdownLine.Rounding -> rounding
            }
        }
    }

    private const val BPS_PER_UNIT = 10_000.0
}
