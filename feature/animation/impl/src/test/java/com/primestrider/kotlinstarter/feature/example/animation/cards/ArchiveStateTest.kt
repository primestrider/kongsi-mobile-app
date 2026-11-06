package com.primestrider.kotlinstarter.feature.example.animation.cards

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveStateTest {
    private fun cards(n: Int = 5) = List(n) { DemoCard(it, 0, it) }

    private fun ids(state: ArchiveState) = state.cards.map { it.id }

    @Test
    fun archive_then_undo_restores_the_card_at_its_index() {
        val state = ArchiveState(cards())
        state.archive(2)
        assertEquals(listOf(0, 1, 3, 4), ids(state))
        assertEquals(2, state.lastArchived?.id)
        state.undo()
        assertEquals(listOf(0, 1, 2, 3, 4), ids(state))
        assertNull(state.lastArchived)
    }

    @Test
    fun undo_after_other_cards_were_archived_returns_every_card_once_within_bounds() {
        val state = ArchiveState(cards())
        state.archive(2)
        state.archive(0)
        state.archive(1)
        assertEquals(listOf(3, 4), ids(state))
        repeat(3) { state.undo() }
        assertEquals(listOf(0, 1, 2, 3, 4), ids(state))
        assertNull(state.lastArchived)
    }

    @Test
    fun undo_clamps_an_out_of_range_index_to_the_end() {
        val state = ArchiveState(cards(2))
        state.restoreArchived(DemoCard(9, 0, 0), index = 7)
        state.undo()
        assertEquals(listOf(0, 1, 9), ids(state))
    }

    @Test
    fun undo_with_nothing_archived_is_a_no_op() {
        val state = ArchiveState(cards())
        state.undo()
        assertEquals(listOf(0, 1, 2, 3, 4), ids(state))
        assertNull(state.lastArchived)
    }

    @Test
    fun archiving_every_card_leaves_an_empty_list_and_reset_restores_all() {
        val state = ArchiveState(cards())
        cards().forEach { state.archive(it.id) }
        assertTrue(state.cards.isEmpty())
        state.reset()
        assertEquals(listOf(0, 1, 2, 3, 4), ids(state))
        assertNull(state.lastArchived)
    }

    @Test
    fun archiving_an_unknown_id_changes_nothing() {
        val state = ArchiveState(cards())
        state.archive(99)
        assertEquals(5, state.cards.size)
        assertNull(state.lastArchived)
    }

    @Test
    fun saver_round_trips_cards_and_last_archived() {
        val state = ArchiveState(cards())
        state.archive(1)
        val saved = with(ArchiveState.Saver) { SaverScope { true }.save(state) }
        assertNotNull(saved)
        val restored = ArchiveState.Saver.restore(saved!!)!!
        assertEquals(state.cards.toList(), restored.cards.toList())
        assertEquals(state.lastArchived, restored.lastArchived)
        restored.undo()
        assertEquals(listOf(0, 1, 2, 3, 4), ids(restored))
        restored.reset()
        assertEquals(5, restored.cards.size)
    }
}
