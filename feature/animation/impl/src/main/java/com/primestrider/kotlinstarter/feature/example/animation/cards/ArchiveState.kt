package com.primestrider.kotlinstarter.feature.example.animation.cards

import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/** A demo card. [title] is a string resource (invented sample content); [colorIndex] picks a gradient. */
data class DemoCard(
    val id: Int,
    @StringRes val title: Int,
    val colorIndex: Int,
)

/**
 * The cards still shown plus a stack of archived ones, so undo can bring back each archive in turn.
 * Undo re-inserts a card at its original index, clamped to the current size, so it is never lost or duplicated.
 */
class ArchiveState(
    initial: List<DemoCard>,
) {
    private val shown = mutableStateListOf<DemoCard>().apply { addAll(initial) }
    private val archived = mutableStateListOf<Pair<DemoCard, Int>>()
    private val all = initial.toList()

    val cards: List<DemoCard> get() = shown

    /** The most recent archived card, which [undo] brings back, or null. */
    val lastArchived: DemoCard? get() = archived.lastOrNull()?.first

    fun archive(id: Int) {
        val index = shown.indexOfFirst { it.id == id }
        if (index < 0) return
        archived.add(shown.removeAt(index) to index)
    }

    fun undo() {
        val (card, index) = archived.removeLastOrNull() ?: return
        shown.add(index.coerceAtMost(shown.size), card)
    }

    fun reset() {
        archived.clear()
        shown.clear()
        shown.addAll(all)
    }

    internal fun restoreArchived(
        card: DemoCard,
        index: Int,
    ) {
        archived.add(card to index)
    }

    companion object {
        private fun DemoCard.toSaved(): List<Any> = listOf(id, title, colorIndex)

        private fun List<*>.toCard() = DemoCard(this[0] as Int, this[1] as Int, this[2] as Int)

        /** Saves the shown cards, the initial cards and the archive stack as plain lists. */
        val Saver: Saver<ArchiveState, Any> =
            listSaver(
                save = { state ->
                    listOf(
                        state.all.map { it.toSaved() },
                        state.shown.map { it.toSaved() },
                        state.archived.map { it.first.toSaved() + it.second },
                    )
                },
                restore = { saved ->
                    val all = (saved[0] as List<*>).map { (it as List<*>).toCard() }
                    ArchiveState(all).also { state ->
                        state.shown.clear()
                        state.shown.addAll((saved[1] as List<*>).map { (it as List<*>).toCard() })
                        (saved[2] as List<*>).forEach {
                            val row = it as List<*>
                            state.restoreArchived(row.toCard(), row[3] as Int)
                        }
                    }
                },
            )
    }
}
