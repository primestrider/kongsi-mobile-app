package com.primestrider.kongsi.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonTonesTest {
    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    @Test
    fun every_person_gets_a_stable_tone_and_eight_people_get_eight_tones() {
        assertEquals(personTone(3, dark = false), personTone(3, dark = false))
        assertEquals(8, (0 until 8).map { personTone(it, dark = false) }.toSet().size)
        assertEquals(personTone(0, dark = true), personTone(8, dark = true))
    }

    @Test
    fun initials_are_readable_on_every_tone_in_both_themes() {
        listOf(false, true).forEach { dark ->
            (0 until 8).forEach { index ->
                val tone = personTone(index, dark)
                val ratio = contrast(tone.container, tone.content)
                assertTrue("tone $index dark=$dark: $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun negative_index_still_has_a_tone() {
        assertEquals(personTone(7, dark = false), personTone(-1, dark = false))
    }
}
