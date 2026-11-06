package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMathTest {
    @Test
    fun moved_reorders_and_ignores_bad_indices() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "c", "a"), list.moved(0, 2))
        assertEquals(listOf("c", "a", "b"), list.moved(2, 0))
        assertEquals(list, list.moved(5, 0))
        assertEquals(list, list.moved(0, -1))
        assertEquals(list, list.moved(1, 1))
    }

    @Test
    fun clamp_pan_locks_at_scale_one() {
        assertEquals(Offset.Zero, clampPan(Offset(50f, 50f), 1f, Size(100f, 100f)))
    }

    @Test
    fun clamp_pan_stops_at_edge() {
        assertEquals(Offset(50f, -50f), clampPan(Offset(500f, -500f), 2f, Size(100f, 100f)))
        assertEquals(Offset(10f, -20f), clampPan(Offset(10f, -20f), 2f, Size(100f, 100f)))
    }

    @Test
    fun toggle_zoom_alternates() {
        assertEquals(DOUBLE_TAP_ZOOM, toggleZoom(MIN_ZOOM))
        assertEquals(MIN_ZOOM, toggleZoom(DOUBLE_TAP_ZOOM))
        assertEquals(MIN_ZOOM, toggleZoom(MAX_ZOOM))
        assertEquals(2.5f, DOUBLE_TAP_ZOOM)
    }

    @Test
    fun swipe_outcome_by_distance_and_velocity() {
        assertEquals(SwipeOutcome.Return, swipeOutcome(50f, 0f, 1000f))
        assertEquals(SwipeOutcome.Left, swipeOutcome(-500f, 0f, 1000f))
        assertEquals(SwipeOutcome.Right, swipeOutcome(500f, 0f, 1000f))
        assertEquals(SwipeOutcome.Right, swipeOutcome(10f, 2000f, 1000f))
        assertEquals(SwipeOutcome.Left, swipeOutcome(10f, -2000f, 1000f))
    }

    @Test
    fun header_consumes_until_collapsed() {
        assertEquals(150f to -50f, headerHeightAfter(-50f, 200f, 64f, 200f))
        assertEquals(64f to -36f, headerHeightAfter(-500f, 100f, 64f, 200f))
        assertEquals(64f to 0f, headerHeightAfter(-10f, 64f, 64f, 200f))
    }

    @Test
    fun header_grows_back_up_to_its_full_height() {
        assertEquals(200f to 36f, headerHeightAfter(500f, 164f, 64f, 200f))
    }

    @Test
    fun compact_strokes_bounds_the_saved_points_and_keeps_every_stroke() {
        val long = List(30_000) { Offset(it.toFloat(), 0f) }
        val strokes = listOf(long, List(5_000) { Offset(0f, it.toFloat()) }, listOf(Offset(1f, 1f)))
        val compact = compactStrokes(strokes, maxPoints = 10_000)
        assertTrue(compact.sumOf { it.size } <= 10_000)
        assertEquals(3, compact.size)
        assertEquals(long.first(), compact[0].first())
        assertEquals(long.last(), compact[0].last())
        assertEquals(listOf(Offset(1f, 1f)), compact[2])
    }

    @Test
    fun compact_strokes_leaves_small_drawings_alone() {
        val strokes = listOf(listOf(Offset(1f, 2f), Offset(3f, 4f)))
        assertEquals(strokes, compactStrokes(strokes, maxPoints = 10_000))
    }
}
