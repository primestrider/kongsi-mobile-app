package com.primestrider.kotlinstarter.feature.example.todos.data

import com.primestrider.kotlinstarter.feature.example.todos.model.Todo
import com.primestrider.kotlinstarter.feature.example.todos.widget.TodosWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/** Todo storage as [Todo] models. */
interface TodosRepository {
    /** Every todo, newest first. */
    val todos: Flow<List<Todo>>

    suspend fun add(title: String)

    suspend fun toggle(id: String)

    suspend fun remove(id: String)

    /** Deletes the done todos; returns how many were deleted. */
    suspend fun clearCompleted(): Int
}

/**
 * [TodosRepository] on Room; [clock] stamps `createdAt` (newest first) and is replaceable in tests. Every write
 * redraws the home-screen widget through [widget].
 */
class DefaultTodosRepository(
    private val dao: TodoDao,
    private val widget: TodosWidgetUpdater,
    private val clock: () -> Long,
) : TodosRepository {
    @Inject
    constructor(dao: TodoDao, widget: TodosWidgetUpdater) : this(dao, widget, System::currentTimeMillis)

    override val todos: Flow<List<Todo>> = dao.observeAll().map { rows -> rows.map { it.toTodo() } }

    override suspend fun add(title: String) {
        dao.insert(TodoEntity(UUID.randomUUID().toString(), title.trim(), done = false, createdAt = clock()))
        widget.update()
    }

    override suspend fun toggle(id: String) {
        dao.toggle(id)
        widget.update()
    }

    override suspend fun remove(id: String) {
        dao.delete(id)
        widget.update()
    }

    override suspend fun clearCompleted(): Int = dao.deleteCompleted().also { widget.update() }
}

fun TodoEntity.toTodo(): Todo = Todo(id = id, title = title, done = done, createdAt = createdAt)
