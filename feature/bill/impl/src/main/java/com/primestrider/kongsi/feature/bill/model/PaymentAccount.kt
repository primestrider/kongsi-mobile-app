package com.primestrider.kongsi.feature.bill.model

enum class PaymentAccountType { BANK, EWALLET }

/**
 * One of the bill creator's own accounts, listed in the WhatsApp message so friends know where to transfer.
 * [number] is stored normalised: digits only for a bank, `08…` for an e-wallet. [id] 0 means not saved yet.
 */
data class PaymentAccount(
    val id: Long = 0,
    val type: PaymentAccountType,
    val provider: String,
    val number: String,
    val holderName: String,
    val isDefault: Boolean,
    val createdAt: Long = 0,
)

/** Common Indonesian providers offered in the picker; anything else is typed in as "Other". */
object PaymentProviders {
    val banks: List<String> =
        listOf(
            "BCA",
            "BRI",
            "BNI",
            "Mandiri",
            "BSI",
            "CIMB Niaga",
            "Permata",
            "Danamon",
            "BTN",
            "Bank Jago",
            "SeaBank",
            "blu by BCA Digital",
            "Jenius",
        )

    val ewallets: List<String> = listOf("GoPay", "OVO", "DANA", "ShopeePay", "LinkAja")

    fun of(type: PaymentAccountType): List<String> =
        when (type) {
            PaymentAccountType.BANK -> banks
            PaymentAccountType.EWALLET -> ewallets
        }
}
