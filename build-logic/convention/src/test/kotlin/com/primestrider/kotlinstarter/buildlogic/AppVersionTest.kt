package com.primestrider.kotlinstarter.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppVersionTest {
    @Test
    fun code_is_major_minor_patch() {
        assertEquals(10203, AppVersion.parse("1.2.3").code)
        assertEquals(1, AppVersion.parse("0.0.1").code)
        assertEquals(209999, AppVersion.parse("20.99.99").code)
    }

    @Test
    fun bump_minor_resets_patch() = assertEquals("1.3.0", AppVersion.parse("1.2.3").bump(Bump.MINOR).toString())

    @Test
    fun bump_major_resets_minor_and_patch() = assertEquals("2.0.0", AppVersion.parse("1.2.3").bump(Bump.MAJOR).toString())

    @Test
    fun bump_patch_and_none() {
        assertEquals("1.2.4", AppVersion.parse("1.2.3").bump(Bump.PATCH).toString())
        assertEquals("1.2.3", AppVersion.parse("1.2.3").bump(Bump.NONE).toString())
    }

    /** 1.2.99 + patch would be 1.2.100, whose code collides with 1.3.0; the minor/major part must be raised instead. */
    @Test
    fun bump_past_99_is_rejected() {
        assertThrows(IllegalArgumentException::class.java) { AppVersion.parse("1.2.99").bump(Bump.PATCH) }
        assertThrows(IllegalArgumentException::class.java) { AppVersion.parse("1.99.0").bump(Bump.MINOR) }
    }

    @Test
    fun parse_rejects_malformed_versions() {
        listOf("1.0", "v1.0.0", "", "1.0.0-beta", "1.100.0", "2100.0.0").forEach { text ->
            assertThrows("accepted \"$text\"", IllegalArgumentException::class.java) { AppVersion.parse(text) }
        }
    }

    @Test
    fun bump_names_parse_case_insensitively() {
        assertEquals(Bump.MINOR, Bump.of("minor"))
        assertEquals(Bump.NONE, Bump.of("none"))
        assertThrows(IllegalArgumentException::class.java) { Bump.of("huge") }
    }
}
