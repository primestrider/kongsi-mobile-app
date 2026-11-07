package com.primestrider.kongsi.feature.bill.model

import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType.BANK
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType.EWALLET
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentAccountValidationTest {
    @Test
    fun bank_number_keeps_digits_and_ignores_separators() {
        assertEquals("1234567890", normalizeAccountNumber(BANK, "123-456-7890"))
        assertEquals("1234567890", normalizeAccountNumber(BANK, " 123 456 7890 "))
        assertEquals("12345", normalizeAccountNumber(BANK, "12345"))
        assertEquals("1".repeat(20), normalizeAccountNumber(BANK, "1".repeat(20)))
    }

    @Test
    fun bank_number_rejects_wrong_length_and_letters() {
        assertNull(normalizeAccountNumber(BANK, "1234"))
        assertNull(normalizeAccountNumber(BANK, "1".repeat(21)))
        assertNull(normalizeAccountNumber(BANK, "12345abc"))
        assertNull(normalizeAccountNumber(BANK, ""))
    }

    @Test
    fun ewallet_number_is_stored_as_local_mobile_number() {
        assertEquals("081234567890", normalizeAccountNumber(EWALLET, "+62 812-3456-7890"))
        assertNull(normalizeAccountNumber(EWALLET, "1234567890"))
    }

    @Test
    fun valid_account_has_no_errors() {
        assertTrue(validatePaymentAccount(BANK, "BCA", "1234567890", "Ricky").isValid)
        assertTrue(validatePaymentAccount(EWALLET, "GoPay", "081234567890", "Ricky").isValid)
    }

    @Test
    fun each_field_reports_its_own_error() {
        val errors = validatePaymentAccount(BANK, " ", "12", "  ")
        assertEquals(R.string.bill_account_error_provider, errors.provider)
        assertEquals(R.string.bill_account_error_bank_number, errors.number)
        assertEquals(R.string.bill_account_error_holder, errors.holderName)
        assertEquals(
            R.string.bill_account_error_ewallet_number,
            validatePaymentAccount(EWALLET, "OVO", "12", "Ricky").number,
        )
    }

    @Test
    fun names_that_are_too_long_are_rejected() {
        val errors = validatePaymentAccount(BANK, "B".repeat(PROVIDER_NAME_MAX + 1), "12345", "R".repeat(HOLDER_NAME_MAX + 1))
        assertEquals(R.string.bill_account_error_provider_long, errors.provider)
        assertEquals(R.string.bill_account_error_holder_long, errors.holderName)
    }
}
