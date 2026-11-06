package com.primestrider.kotlinstarter.feature.example.animation.lists

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DemoListStateTest {
    private fun state() = DemoListState(listOf(DemoRow(1), DemoRow(2), DemoRow(3)))

    private fun DemoListState.ids() = rows.map { it.id }

    @Test
    fun add_twice_gives_unique_ids_newest_first() {
        val s = state()
        s.add()
        s.add()
        assertEquals(listOf(5, 4, 1, 2, 3), s.ids())
    }

    @Test
    fun rapid_adds_and_removes_keep_ids_unique() {
        val s = state()
        repeat(5) { s.add() }
        s.remove(8)
        s.remove(1)
        repeat(3) { s.add() }
        assertEquals(s.ids().size, s.ids().toSet().size)
        // An id is never reused after its row was removed.
        assertTrue(s.ids().max() == 11)
    }

    @Test
    fun add_after_removing_everything_still_gets_a_new_id() {
        val s = state()
        s.ids().forEach { s.remove(it) }
        s.add()
        assertEquals(listOf(4), s.ids())
    }

    @Test
    fun add_on_an_empty_list_starts_at_one() {
        val s = DemoListState(emptyList())
        s.add()
        assertEquals(listOf(1), s.ids())
    }

    @Test
    fun moving_past_the_edges_is_a_no_op() {
        val s = state()
        s.moveUp(1)
        s.moveDown(3)
        assertEquals(listOf(1, 2, 3), s.ids())
    }

    @Test
    fun move_down_and_up_swap_neighbours() {
        val s = state()
        s.moveDown(1)
        assertEquals(listOf(2, 1, 3), s.ids())
        s.moveUp(3)
        assertEquals(listOf(2, 3, 1), s.ids())
    }

    @Test
    fun moving_an_unknown_id_changes_nothing() {
        val s = state()
        s.moveUp(99)
        s.moveDown(99)
        s.remove(99)
        assertEquals(listOf(1, 2, 3), s.ids())
    }

    @Test
    fun shuffle_keeps_the_same_ids() {
        val s = state()
        repeat(3) { s.add() }
        s.shuffle(Random(42))
        assertEquals((1..6).toList(), s.ids().sorted())
    }

    @Test
    fun removing_all_leaves_an_empty_list() {
        val s = state()
        s.ids().forEach { s.remove(it) }
        assertTrue(s.rows.isEmpty())
    }

    @Test
    fun saver_round_trip_keeps_order_and_next_id() {
        val s = state()
        s.add()
        s.remove(4)
        s.moveDown(1)
        val saved = with(DemoListState.Saver) { SaverScope { true }.save(s) }!!
        val restored = DemoListState.Saver.restore(saved)!!
        assertEquals(s.ids(), restored.ids())
        // The removed id 4 is not handed out again after a restore.
        restored.add()
        assertEquals(5, restored.ids().first())
    }
}
