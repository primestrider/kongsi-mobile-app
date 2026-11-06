package com.primestrider.kotlinstarter.feature.example.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Stand-in for a network call, matching the RN example. */
internal const val SUBMIT_DELAY_MS = 1500L

data class FormUiState(
    val values: FormValues = FormValues(),
    val errors: Map<FormField, Int> = emptyMap(),
    val submitAttempted: Boolean = false,
    val submitting: Boolean = false,
    val submitted: FormValues? = null,
)

/**
 * Validates on submit, then re-validates on every change (react-hook-form's default),
 * so errors never appear while the user is still filling the form for the first time.
 */
@HiltViewModel
class FormExampleViewModel
    @Inject
    constructor() : ViewModel() {
        private val _state = MutableStateFlow(FormUiState())
        val state: StateFlow<FormUiState> = _state.asStateFlow()

        private var submitJob: Job? = null

        fun onChange(
            field: FormField,
            value: String,
        ) {
            _state.update { current ->
                val values =
                    when (field) {
                        FormField.NAME -> current.values.copy(name = value)
                        FormField.EMAIL -> current.values.copy(email = value)
                        FormField.PHONE -> current.values.copy(phone = value)
                        FormField.PASSWORD -> current.values.copy(password = value)
                        FormField.BIO -> current.values.copy(bio = value)
                    }
                current.copy(values = values, errors = if (current.submitAttempted) validate(values) else emptyMap())
            }
        }

        fun submit() {
            val current = _state.value
            if (current.submitting) return
            val errors = validate(current.values)
            _state.update { it.copy(submitAttempted = true, errors = errors) }
            if (errors.isNotEmpty()) return

            val submittedValues = current.values
            _state.update { it.copy(submitting = true) }
            submitJob =
                viewModelScope.launch {
                    delay(SUBMIT_DELAY_MS)
                    _state.update { it.copy(submitting = false, submitted = submittedValues) }
                }
        }

        fun clear() {
            submitJob?.cancel()
            _state.value = FormUiState()
        }
    }
