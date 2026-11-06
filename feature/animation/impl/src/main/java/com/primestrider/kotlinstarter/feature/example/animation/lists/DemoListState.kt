package com.primestrider.kotlinstarter.feature.example.animation.lists

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlin.random.Random

/** A demo row. Only the id is stored; the "Item <id>" label is formatted from a string resource when shown. */
data class DemoRow(
    val id: Int,
)

/**
 * The rows of the animated-list example. New rows get an id that was never used before, so ids stay
 * unique even after the newest row is removed, and rapid add/remove cannot produce duplicate keys.
 */
class DemoListState(
    initial: List<DemoRow>,
    nextId: Int = 0,
) {
    private val items = mutableStateListOf<DemoRow>().apply { addAll(initial) }
    private var nextId = maxOf(nextId, (initial.maxOfOrNull { it.id } ?: 0) + 1)

    val rows: List<DemoRow> get() = items

    /** Inserts a new row at the top. */
    fun add() {
        items.add(0, DemoRow(nextId++))
    }

    fun remove(id: Int) {
        items.removeAll { it.id == id }
    }

    /** Moves the row one place towards the top; does nothing at the top or for an unknown id. */
    fun moveUp(id: Int) = swap(id, -1)

    /** Moves the row one place towards the bottom; does nothing at the bottom or for an unknown id. */
    fun moveDown(id: Int) = swap(id, 1)

    fun shuffle(random: Random) {
        val shuffled = items.shuffled(random)
        items.clear()
        items.addAll(shuffled)
    }

    private fun swap(
        id: Int,
        delta: Int,
    ) {
        val from = items.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in items.indices) return
        items.add(to, items.removeAt(from))
    }

    companion object {
        /** Saves the next id followed by the row ids, in order. */
        val Saver: Saver<DemoListState, Any> =
            listSaver(
                save = { state -> listOf(state.nextId) + state.items.map { it.id } },
                restore = { saved -> DemoListState(saved.drop(1).map { DemoRow(it as Int) }, saved[0] as Int) },
            )
    }
}
