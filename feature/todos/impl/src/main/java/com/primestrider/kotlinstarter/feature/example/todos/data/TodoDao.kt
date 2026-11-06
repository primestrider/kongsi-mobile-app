package com.primestrider.kotlinstarter.feature.example.todos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    /** Emits again after every change to the table. */
    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Insert
    suspend fun insert(todo: TodoEntity)

    @Query("UPDATE todos SET done = NOT done WHERE id = :id")
    suspend fun toggle(id: String)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: String)

    /** Returns how many rows were deleted. */
    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun deleteCompleted(): Int
}
