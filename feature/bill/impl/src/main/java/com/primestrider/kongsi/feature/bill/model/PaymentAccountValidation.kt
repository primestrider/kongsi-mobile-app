package com.primestrider.kongsi.feature.bill.model

import androidx.annotation.StringRes
import com.primestrider.kongsi.core.util.IndonesianPhone
import com.primestrider.kongsi.feature.bill.impl.R

private const val BANK_NUMBER_MIN = 5
private const val BANK_NUMBER_MAX = 20
private const val BANK_SEPARATORS = " -."
const val HOLDER_NAME_MAX = 50
const val PROVIDER_NAME_MAX = 30

/** Field errors of a payment account form; null means the field is fine. */
data class PaymentAccountErrors(
    @StringRes val provider: Int? = null,
    @StringRes val number: Int? = null,
    @StringRes val holderName: Int? = null,
) {
    val isValid: Boolean get() = provider == null && number == null && holderName == null
}

/**
 * The number as it is stored and sent: digits only for a bank (5–20 digits; spaces, dashes and dots ignored), `08…`
 * for an e-wallet (an Indonesian mobile number). Null when it is not valid for [type].
 */
fun normalizeAccountNumber(
    type: PaymentAccountType,
    raw: String,
): String? =
    when (type) {
        PaymentAccountType.BANK -> {
            val text = raw.trim()
            text
                .filter(Char::isDigit)
                .takeIf { digits ->
                    text.all { it.isDigit() || it in BANK_SEPARATORS } && digits.length in BANK_NUMBER_MIN..BANK_NUMBER_MAX
                }
        }
        PaymentAccountType.EWALLET -> IndonesianPhone.local(raw)
    }

fun validatePaymentAccount(
    type: PaymentAccountType,
    provider: String,
    number: String,
    holderName: String,
): PaymentAccountErrors =
    PaymentAccountErrors(
        provider =
            when {
                provider.isBlank() -> R.string.bill_account_error_provider
                provider.trim().length > PROVIDER_NAME_MAX -> R.string.bill_account_error_provider_long
                else -> null
            },
        number =
            if (normalizeAccountNumber(type, number) != null) {
                null
            } else {
                when (type) {
                    PaymentAccountType.BANK -> R.string.bill_account_error_bank_number
                    PaymentAccountType.EWALLET -> R.string.bill_account_error_ewallet_number
                }
            },
        holderName =
            when {
                holderName.isBlank() -> R.string.bill_account_error_holder
                holderName.trim().length > HOLDER_NAME_MAX -> R.string.bill_account_error_holder_long
                else -> null
            },
    )
