package com.primestrider.kotlinstarter.feature.example.form

import androidx.annotation.StringRes
import com.primestrider.kotlinstarter.feature.catalog.impl.R

enum class FormField { NAME, EMAIL, PHONE, PASSWORD, BIO }

data class FormValues(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val bio: String = "",
)

private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private const val PHONE_MIN = 10
private const val PASSWORD_MIN = 8
internal const val BIO_MAX = 200

/**
 * Rules of the RN form example (`form.tsx`). Errors are string resources, not sentences,
 * so a message follows a language switch. "Required" means blank after trimming.
 */
fun validate(values: FormValues): Map<FormField, Int> =
    buildMap {
        fun error(
            field: FormField,
            @StringRes message: Int,
        ) = put(field, message)

        if (values.name.isBlank()) error(FormField.NAME, R.string.form_error_name_required)

        when {
            values.email.isBlank() -> error(FormField.EMAIL, R.string.form_error_email_required)
            !EMAIL_PATTERN.matches(values.email) -> error(FormField.EMAIL, R.string.form_error_email_invalid)
        }

        when {
            values.phone.isBlank() -> error(FormField.PHONE, R.string.form_error_phone_required)
            values.phone.length < PHONE_MIN -> error(FormField.PHONE, R.string.form_error_phone_min)
        }

        when {
            values.password.isBlank() -> error(FormField.PASSWORD, R.string.form_error_password_required)
            values.password.length < PASSWORD_MIN -> error(FormField.PASSWORD, R.string.form_error_password_min)
        }

        if (values.bio.length > BIO_MAX) error(FormField.BIO, R.string.form_error_bio_max)
    }
