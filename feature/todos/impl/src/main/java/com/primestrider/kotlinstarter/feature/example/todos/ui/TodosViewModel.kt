package com.primestrider.kotlinstarter.feature.example.todos.ui

import android.database.sqlite.SQLiteException
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.component.UiText
import com.primestrider.kotlinstarter.feature.example.todos.data.TodosRepository
import com.primestrider.kotlinstarter.feature.example.todos.model.Todo
import com.primestrider.kotlinstarter.feature.todos.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val INPUT_KEY = "input"
private const val FILTER_KEY = "filter"
private const val SUBMITTED_KEY = "submitted"
private const val TITLE_MIN = 3
private const val TITLE_MAX = 100

enum class TodoFilter { ALL, ACTIVE, DONE }

/** The error for a todo title, or null when it is fine; length is counted after trimming. */
@StringRes
fun validateTodoTitle(title: String): Int? {
    val length = title.trim().length
    return when {
        length < TITLE_MIN -> R.string.todos_validation_min
        length > TITLE_MAX -> R.string.todos_validation_max
        else -> null
    }
}

data class TodosUiState(
    val input: String,
    @StringRes val inputError: Int?,
    val filter: TodoFilter,
    val todos: List<Todo>,
    val done: Int,
    val total: Int,
    /** False until the database has answered once, so the empty state does not flash on open. */
    val loaded: Boolean = true,
)

/** Todos from Room plus the draft and filter, which live in [SavedStateHandle] and survive process death. */
@HiltViewModel
class TodosViewModel
    @Inject
    constructor(
        private val repository: TodosRepository,
        private val savedStateHandle: SavedStateHandle,
        private val messenger: Messenger,
    ) : ViewModel() {
        val state: StateFlow<TodosUiState> =
            combine(
                savedStateHandle.getStateFlow(INPUT_KEY, ""),
                // Errors appear on the first submit, then follow the input live.
                savedStateHandle.getStateFlow(SUBMITTED_KEY, false),
                savedStateHandle.getStateFlow(FILTER_KEY, TodoFilter.ALL),
                // null until the database answers, so typing is never held back by the first query.
                repository.todos.onStart<List<Todo>?> { emit(null) },
            ) { input, submitted, filter, stored ->
                val all = stored.orEmpty()
                TodosUiState(
                    input = input,
                    inputError = if (submitted) validateTodoTitle(input) else null,
                    filter = filter,
                    todos =
                        when (filter) {
                            TodoFilter.ALL -> all
                            TodoFilter.ACTIVE -> all.filterNot { it.done }
                            TodoFilter.DONE -> all.filter { it.done }
                        },
                    done = all.count { it.done },
                    total = all.size,
                    loaded = stored != null,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                TodosUiState("", null, TodoFilter.ALL, emptyList(), done = 0, total = 0, loaded = false),
            )

        fun onInputChange(value: String) {
            savedStateHandle[INPUT_KEY] = value
        }

        fun setFilter(filter: TodoFilter) {
            savedStateHandle[FILTER_KEY] = filter
        }

        fun add() {
            val title = savedStateHandle.get<String>(INPUT_KEY).orEmpty()
            savedStateHandle[SUBMITTED_KEY] = true
            if (validateTodoTitle(title) != null) return
            // Clear before writing so a double tap cannot add the same task twice.
            savedStateHandle[INPUT_KEY] = ""
            savedStateHandle[SUBMITTED_KEY] = false
            write(onFailure = { savedStateHandle[INPUT_KEY] = title }) {
                repository.add(title)
                messenger.success(UiText.Resource(R.string.todos_added))
            }
        }

        fun toggle(id: String) = write { repository.toggle(id) }

        fun remove(id: String) =
            write {
                repository.remove(id)
                messenger.success(UiText.Resource(R.string.todos_removed))
            }

        fun clearCompleted() =
            write {
                if (repository.clearCompleted() > 0) messenger.success(UiText.Resource(R.string.todos_cleared))
            }

        /** Runs a database write; a failure (disk full, corrupt file) is reported instead of crashing the app. */
        private fun write(
            onFailure: () -> Unit = {},
            block: suspend () -> Unit,
        ) {
            viewModelScope.launch {
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SQLiteException) {
                    onFailure()
                    messenger.error(UiText.Resource(R.string.todos_save_failed))
                }
            }
        }
    }
