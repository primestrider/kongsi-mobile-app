package com.primestrider.kongsi.feature.bill

import kotlinx.serialization.Serializable

// Routes of the bill feature. Home itself is the app's HomeRoute; the bill feature renders it (BillHome).

/** The bill creator's own bank accounts and e-wallets, shown in the WhatsApp message. */
@Serializable
data object PaymentAccountsRoute

/** Adds a payment account ([id] null) or edits the one with [id]. */
@Serializable
data class PaymentAccountEditRoute(
    val id: Long? = null,
)
