package com.primestrider.kotlinstarter.feature.example.sdk.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

private const val AMOUNT_KEY = "amount"
const val CODE_LIFETIME_SECONDS = 60

data class PaymentCodeState(
    val code: String,
    val secondsLeft: Int = CODE_LIFETIME_SECONDS,
)

/**
 * The payment code and its 60 second countdown. It ticks every second; at 0 a new code replaces it and the
 * countdown restarts. [amount] comes from the navigation arguments (the deep link's `amount` query); that is
 * untrusted outside input, so a negative value is treated as absent.
 */
@HiltViewModel
class PaymentCodeViewModel(
    savedStateHandle: SavedStateHandle,
    private val random: Random,
) : ViewModel() {
    @Inject
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle, Random.Default)

    val amount: Long? = savedStateHandle.get<Long>(AMOUNT_KEY)?.takeIf { it >= 0 }

    private val _state = MutableStateFlow(PaymentCodeState(paymentCode(random)))
    val state: StateFlow<PaymentCodeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _state.update { current ->
                    if (current.secondsLeft > 1) {
                        current.copy(secondsLeft = current.secondsLeft - 1)
                    } else {
                        PaymentCodeState(paymentCode(random))
                    }
                }
            }
        }
    }
}
