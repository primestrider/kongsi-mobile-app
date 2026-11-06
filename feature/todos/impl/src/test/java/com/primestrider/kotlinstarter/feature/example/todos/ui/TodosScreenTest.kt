package com.primestrider.kotlinstarter.feature.example.todos.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.feature.example.todos.model.Todo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TodosScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val removed = mutableListOf<String>()
    private var added = 0

    private fun show(todos: List<Todo>) =
        rule.setContent {
            AppTheme(false) {
                TodosScreen(
                    state =
                        TodosUiState(
                            input = "",
                            inputError = null,
                            filter = TodoFilter.ALL,
                            todos = todos,
                            done = todos.count { it.done },
                            total = todos.size,
                        ),
                    onInputChange = {},
                    onAdd = { added++ },
                    onToggle = {},
                    onRemove = { removed += it },
                    onClearCompleted = {},
                    onFilterChange = {},
                    onBack = {},
                )
            }
        }

    @Test
    fun empty_list_shows_empty_state() {
        show(emptyList())
        rule.onNodeWithText("Nothing to do").assertExists()
    }

    @Test
    fun delete_asks_first_and_only_confirm_removes() {
        show(listOf(Todo("a", "Walk the dog", done = false, createdAt = 1)))
        rule.onNodeWithContentDescription("Delete Walk the dog").performClick()
        rule.onNodeWithText("Delete this task?").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(emptyList<String>(), removed)

        rule.onNodeWithContentDescription("Delete Walk the dog").performClick()
        rule.onNodeWithText("Delete").performClick()
        assertEquals(listOf("a"), removed)
    }

    @Test
    fun ime_done_adds() {
        show(emptyList())
        rule.onNodeWithTag(TODO_INPUT_TAG).performImeAction()
        assertEquals(1, added)
    }

    @Test
    fun clear_completed_disabled_when_nothing_done() {
        show(listOf(Todo("a", "Walk the dog", done = false, createdAt = 1)))
        rule.onNodeWithText("Clear completed").assertIsNotEnabled()
    }
}
