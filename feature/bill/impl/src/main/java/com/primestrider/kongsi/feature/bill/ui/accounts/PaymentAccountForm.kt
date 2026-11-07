package com.primestrider.kongsi.feature.bill.ui.accounts

import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import java.io.Serializable

/** What the payment account form keeps in the `SavedStateHandle` (a `Serializable`, so a Bundle can hold it). */
data class PaymentAccountForm(
    val type: PaymentAccountType,
    val providerChoice: String?,
    val otherProvider: String,
    val number: String,
    val holderName: String,
    val isDefault: Boolean,
    val submitted: Boolean,
    val createdAt: Long,
) : Serializable {
    fun toState(isNew: Boolean): PaymentAccountEditUiState =
        PaymentAccountEditUiState(
            isNew = isNew,
            loaded = true,
            type = type,
            providerChoice = providerChoice,
            otherProvider = otherProvider,
            number = number,
            holderName = holderName,
            isDefault = isDefault,
            submitted = submitted,
        )

    companion object {
        private const val serialVersionUID = 1L

        fun from(
            state: PaymentAccountEditUiState,
            createdAt: Long,
        ): PaymentAccountForm =
            PaymentAccountForm(
                type = state.type,
                providerChoice = state.providerChoice,
                otherProvider = state.otherProvider,
                number = state.number,
                holderName = state.holderName,
                isDefault = state.isDefault,
                submitted = state.submitted,
                createdAt = createdAt,
            )
    }
}
