package com.primestrider.kotlinstarter.feature.example.form

import com.primestrider.kotlinstarter.feature.catalog.impl.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormValidationTest {
    private val valid =
        FormValues(
            name = "Ada Lovelace",
            email = "ada@example.com",
            phone = "0812345678",
            password = "analytical",
            bio = "",
        )

    @Test
    fun empty_form_requires_all_but_bio() {
        val errors = validate(FormValues())
        assertEquals(setOf(FormField.NAME, FormField.EMAIL, FormField.PHONE, FormField.PASSWORD), errors.keys)
        assertEquals(R.string.form_error_name_required, errors[FormField.NAME])
        assertEquals(R.string.form_error_email_required, errors[FormField.EMAIL])
        assertEquals(R.string.form_error_phone_required, errors[FormField.PHONE])
        assertEquals(R.string.form_error_password_required, errors[FormField.PASSWORD])
    }

    @Test
    fun blank_name_is_required() {
        assertEquals(R.string.form_error_name_required, validate(valid.copy(name = "   "))[FormField.NAME])
    }

    @Test
    fun email_must_look_like_an_address() {
        assertEquals(R.string.form_error_email_invalid, validate(valid.copy(email = "a@b"))[FormField.EMAIL])
    }

    @Test
    fun phone_needs_ten_characters() {
        assertEquals(R.string.form_error_phone_min, validate(valid.copy(phone = "123456789"))[FormField.PHONE])
        assertNull(validate(valid.copy(phone = "1234567890"))[FormField.PHONE])
    }

    @Test
    fun password_needs_eight_characters() {
        assertEquals(R.string.form_error_password_min, validate(valid.copy(password = "1234567"))[FormField.PASSWORD])
    }

    @Test
    fun bio_is_capped_at_200() {
        assertEquals(R.string.form_error_bio_max, validate(valid.copy(bio = "x".repeat(201)))[FormField.BIO])
        assertNull(validate(valid.copy(bio = "x".repeat(200)))[FormField.BIO])
    }

    @Test
    fun valid_values_have_no_errors() {
        assertTrue(validate(valid).isEmpty())
    }
}
