package com.primestrider.kotlinstarter.feature.example.todos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppCheckbox
import com.primestrider.kotlinstarter.core.designsystem.component.AppScaffold
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ConfirmDialog
import com.primestrider.kotlinstarter.core.designsystem.component.EmptyState
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.example.todos.model.Todo
import com.primestrider.kotlinstarter.feature.todos.impl.R

const val TODO_INPUT_TAG = "todo_input"

private val filterLabels =
    mapOf(
        TodoFilter.ALL to R.string.todos_filter_all,
        TodoFilter.ACTIVE to R.string.todos_filter_active,
        TodoFilter.DONE to R.string.todos_filter_done,
    )

/** Offline todo list backed by Room: add, toggle, filter, delete (with confirmation) and clear completed. */
@Composable
fun TodosScreen(
    state: TodosUiState,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onFilterChange: (TodoFilter) -> Unit,
    onBack: () -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    AppScaffold(title = stringResource(R.string.todos_title), onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            Text(
                stringResource(R.string.todos_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.Top) {
                AppTextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    label = stringResource(R.string.todos_placeholder),
                    errorText = state.inputError?.let { stringResource(it) },
                    imeAction = ImeAction.Done,
                    onImeAction = onAdd,
                    modifier = Modifier.weight(1f).testTag(TODO_INPUT_TAG),
                )
                AppButton(stringResource(R.string.todos_add), onClick = onAdd, modifier = Modifier.padding(top = Spacing.s2))
            }
            Text(stringResource(R.string.todos_progress, state.done, state.total), style = MaterialTheme.typography.labelLarge)
            val labels = TodoFilter.entries.associateWith { stringResource(filterLabels.getValue(it)) }
            SegmentedChoice(TodoFilter.entries, state.filter, onFilterChange, label = { labels.getValue(it) })
            when {
                !state.loaded -> Unit
                state.todos.isEmpty() ->
                    EmptyState(
                        title = stringResource(R.string.todos_empty_title),
                        description = stringResource(R.string.todos_empty_description),
                        modifier = Modifier.weight(1f),
                    )
                else ->
                    LazyColumn(Modifier.weight(1f)) {
                        items(state.todos, key = { it.id }) { todo ->
                            TodoRow(todo, onToggle = { onToggle(todo.id) }, onDelete = { pendingDelete = todo.id })
                        }
                    }
            }
            AppButton(
                stringResource(R.string.todos_clear_completed),
                onClick = onClearCompleted,
                variant = ButtonVariant.TEXT,
                enabled = state.done > 0,
                modifier = Modifier.align(Alignment.End).padding(bottom = Spacing.s2),
            )
        }
    }
    pendingDelete?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.todos_remove_title),
            message = stringResource(R.string.todos_remove_description),
            confirmLabel = stringResource(R.string.todos_delete),
            onConfirm = {
                pendingDelete = null
                onRemove(id)
            },
            onDismiss = { pendingDelete = null },
            destructive = true,
        )
    }
}

@Composable
private fun TodoRow(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .toggleable(value = todo.done, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(vertical = Spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppCheckbox(checked = todo.done, onCheckedChange = null, modifier = Modifier.padding(Spacing.s3))
            Text(
                todo.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.todos_cd_delete, todo.title))
        }
    }
}

@ThemePreviews
@Composable
private fun TodosScreenPreview() {
    PreviewTheme {
        TodosScreen(
            state =
                TodosUiState(
                    input = "",
                    inputError = null,
                    filter = TodoFilter.ALL,
                    todos =
                        listOf(
                            Todo("1", "Buy milk", done = false, createdAt = 1L),
                            Todo("2", "Write the report", done = true, createdAt = 2L),
                        ),
                    done = 1,
                    total = 2,
                ),
            onInputChange = {},
            onAdd = {},
            onToggle = {},
            onRemove = {},
            onClearCompleted = {},
            onFilterChange = {},
            onBack = {},
        )
    }
}
