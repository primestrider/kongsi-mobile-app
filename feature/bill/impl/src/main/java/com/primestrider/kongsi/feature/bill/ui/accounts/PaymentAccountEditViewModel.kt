package com.primestrider.kongsi.feature.bill.ui.accounts

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.datastore.UserProfileRepository
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.data.PaymentAccountRepository
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountErrors
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import com.primestrider.kongsi.feature.bill.model.PaymentProviders
import com.primestrider.kongsi.feature.bill.model.normalizeAccountNumber
import com.primestrider.kongsi.feature.bill.model.validatePaymentAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The provider picker's "Other" entry; the name is then typed into [PaymentAccountEditUiState.otherProvider]. */
const val OTHER_PROVIDER = "__other__"

/** Route argument of `PaymentAccountEditRoute`; navigation puts it in the [SavedStateHandle]. */
private const val ID_KEY = "id"
private const val FORM_KEY = "form"

data class PaymentAccountEditUiState(
    val isNew: Boolean,
    /** False while an existing account is being read, so the form does not show empty fields first. */
    val loaded: Boolean,
    val type: PaymentAccountType = PaymentAccountType.BANK,
    /** A name from [PaymentProviders], [OTHER_PROVIDER], or null before the user picks one. */
    val providerChoice: String? = null,
    val otherProvider: String = "",
    val number: String = "",
    val holderName: String = "",
    val isDefault: Boolean = false,
    /** Errors show only after the first Save, then follow the input live. */
    val submitted: Boolean = false,
    val saving: Boolean = false,
    /** Saved, or the account to edit no longer exists: the screen goes back. */
    val closed: Boolean = false,
    /** The user's own number from Profile, offered for e-wallets ("Use my number"); null when not set. */
    val myPhone: String? = null,
) {
    val provider: String get() = if (providerChoice == OTHER_PROVIDER) otherProvider.trim() else providerChoice.orEmpty()

    val errors: PaymentAccountErrors get() = validatePaymentAccount(type, provider, number, holderName)

    val shownErrors: PaymentAccountErrors get() = if (submitted) errors else PaymentAccountErrors()
}

/**
 * Adds or edits one payment account. The form lives in [SavedStateHandle] (as a [PaymentAccountForm] bundle), so it
 * survives rotation and process death; an existing account is read once and then edited in the form.
 */
@HiltViewModel
class PaymentAccountEditViewModel
    @Inject
    constructor(
        private val repository: PaymentAccountRepository,
        private val savedStateHandle: SavedStateHandle,
        private val messenger: Messenger,
        profileRepository: UserProfileRepository,
    ) : ViewModel() {
        private val accountId: Long? = savedStateHandle.get<Long>(ID_KEY)
        private val restored: PaymentAccountForm? = savedStateHandle.get<PaymentAccountForm>(FORM_KEY)

        private val _state =
            MutableStateFlow(
                restored?.toState(isNew = accountId == null)
                    ?: PaymentAccountEditUiState(isNew = accountId == null, loaded = accountId == null),
            )
        val state: StateFlow<PaymentAccountEditUiState> = _state.asStateFlow()

        /** Kept from the stored account, so editing does not move it in the list. */
        private var createdAt: Long = restored?.createdAt ?: 0L

        init {
            viewModelScope.launch {
                profileRepository.profile.collect { profile -> _state.update { it.copy(myPhone = profile.phone) } }
            }
            if (restored == null) {
                viewModelScope.launch {
                    if (accountId == null) {
                        // The first account is the default one: there is nothing else to send.
                        if (repository.accounts.first().isEmpty()) update { it.copy(isDefault = true) }
                    } else {
                        val account = repository.get(accountId)
                        if (account == null) {
                            update { it.copy(loaded = true, closed = true) }
                        } else {
                            createdAt = account.createdAt
                            update { account.toState() }
                        }
                    }
                }
            }
        }

        fun onTypeChange(type: PaymentAccountType) =
            update {
                if (it.type == type) it else it.copy(type = type, providerChoice = null, otherProvider = "")
            }

        fun onProviderChange(choice: String) = update { it.copy(providerChoice = choice) }

        fun onOtherProviderChange(value: String) = update { it.copy(otherProvider = value) }

        fun onNumberChange(value: String) = update { it.copy(number = value) }

        fun onUseMyNumber() = update { state -> state.myPhone?.let { state.copy(number = it) } ?: state }

        fun onHolderNameChange(value: String) = update { it.copy(holderName = value) }

        fun onDefaultChange(value: Boolean) = update { it.copy(isDefault = value) }

        fun save() {
            val current = _state.value
            if (current.saving || !current.loaded) return
            update { it.copy(submitted = true) }
            if (!current.errors.isValid) return
            val account =
                PaymentAccount(
                    id = accountId ?: 0L,
                    type = current.type,
                    provider = current.provider,
                    number = requireNotNull(normalizeAccountNumber(current.type, current.number)),
                    holderName = current.holderName.trim(),
                    isDefault = current.isDefault,
                    createdAt = createdAt,
                )
            update { it.copy(saving = true) }
            viewModelScope.launch {
                try {
                    repository.save(account)
                    messenger.success(UiText.Resource(R.string.bill_account_saved))
                    update { it.copy(saving = false, closed = true) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SQLiteException) {
                    update { it.copy(saving = false) }
                    messenger.error(UiText.Resource(R.string.bill_account_save_failed))
                }
            }
        }

        private fun update(change: (PaymentAccountEditUiState) -> PaymentAccountEditUiState) {
            _state.update(change)
            val state = _state.value
            if (state.loaded) savedStateHandle[FORM_KEY] = PaymentAccountForm.from(state, createdAt)
        }

        private fun PaymentAccount.toState(): PaymentAccountEditUiState {
            val known = provider in PaymentProviders.of(type)
            return PaymentAccountEditUiState(
                isNew = false,
                loaded = true,
                type = type,
                providerChoice = if (known) provider else OTHER_PROVIDER,
                otherProvider = if (known) "" else provider,
                number = number,
                holderName = holderName,
                isDefault = isDefault,
            )
        }
    }
