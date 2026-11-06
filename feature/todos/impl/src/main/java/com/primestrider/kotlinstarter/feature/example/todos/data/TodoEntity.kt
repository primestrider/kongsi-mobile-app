package com.primestrider.kotlinstarter.feature.example.todos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val done: Boolean,
    val createdAt: Long,
)
