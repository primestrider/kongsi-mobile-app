package com.primestrider.kotlinstarter.feature.example.todos.model

/** A todo as the screens use it. Room's `TodoEntity` is mapped to this in `data/`, so the table can change alone. */
data class Todo(
    val id: String,
    val title: String,
    val done: Boolean,
    val createdAt: Long,
)
