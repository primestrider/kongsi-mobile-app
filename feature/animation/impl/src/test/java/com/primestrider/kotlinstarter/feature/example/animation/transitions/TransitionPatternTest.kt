package com.primestrider.kotlinstarter.feature.example.animation.transitions

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionPatternTest {
    @Test
    fun every_pattern_has_a_label_and_an_api() {
        TransitionPattern.entries.forEach { pattern ->
            assertNotEquals("label of $pattern", 0, pattern.label)
            assertTrue("api of $pattern", pattern.api.isNotBlank())
        }
    }

    @Test
    fun labels_are_unique() {
        assertEquals(
            TransitionPattern.entries.size,
            TransitionPattern.entries
                .map { it.label }
                .toSet()
                .size,
        )
    }

    @Test
    fun container_transform_keeps_the_list_under_the_morph_and_fades_only_the_page() {
        val p = TransitionPattern.CONTAINER_TRANSFORM
        assertEquals(ExitTransition.None, p.exit(forward = true, slidePx = 0, LayoutDirection.Ltr))
        assertEquals(EnterTransition.None, p.enter(forward = false, slidePx = 0, LayoutDirection.Ltr))
        assertNotEquals(EnterTransition.None, p.enter(forward = true, slidePx = 0, LayoutDirection.Ltr))
        assertNotEquals(ExitTransition.None, p.exit(forward = false, slidePx = 0, LayoutDirection.Ltr))
    }

    @Test
    fun every_other_pattern_animates_both_directions() {
        TransitionPattern.entries.filter { it != TransitionPattern.CONTAINER_TRANSFORM }.forEach { p ->
            listOf(true, false).forEach { forward ->
                assertNotEquals("$p enter", EnterTransition.None, p.enter(forward, 30, LayoutDirection.Ltr))
                assertNotEquals("$p exit", ExitTransition.None, p.exit(forward, 30, LayoutDirection.Ltr))
            }
        }
    }

    @Test
    fun motion_spec_uses_the_material_motion_values() {
        assertEquals(300, MotionSpec.SHARED_AXIS_MS)
        assertEquals(300, MotionSpec.FADE_THROUGH_MS)
        assertEquals(90, MotionSpec.FADE_THROUGH_OUT_MS)
        assertEquals(150, MotionSpec.FADE_IN_MS)
        assertEquals(75, MotionSpec.FADE_OUT_MS)
        assertEquals(300, MotionSpec.CONTAINER_MS)
        assertEquals(30.dp, MotionSpec.SHARED_AXIS_SLIDE)
        assertEquals(0.8f, MotionSpec.SHARED_AXIS_Z_SCALE_IN)
        assertEquals(1.1f, MotionSpec.SHARED_AXIS_Z_SCALE_OUT)
        assertEquals(0.92f, MotionSpec.FADE_THROUGH_SCALE_IN)
    }
}
