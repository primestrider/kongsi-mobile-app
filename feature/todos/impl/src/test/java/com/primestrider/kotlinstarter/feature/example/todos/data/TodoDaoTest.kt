package com.primestrider.kotlinstarter.feature.example.todos.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TodoDaoTest {
    private lateinit var database: ExampleDatabase
    private lateinit var dao: TodoDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ExampleDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.todoDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun newest_first() =
        runTest {
            dao.insert(TodoEntity("a", "Old", done = false, createdAt = 1))
            dao.insert(TodoEntity("b", "New", done = false, createdAt = 2))
            assertEquals(listOf("b", "a"), dao.observeAll().first().map { it.id })
        }

    @Test
    fun toggle_flips_done() =
        runTest {
            dao.insert(TodoEntity("a", "Task", done = false, createdAt = 1))
            dao.toggle("a")
            assertEquals(
                true,
                dao
                    .observeAll()
                    .first()
                    .single()
                    .done,
            )
            dao.toggle("a")
            assertEquals(
                false,
                dao
                    .observeAll()
                    .first()
                    .single()
                    .done,
            )
        }

    @Test
    fun delete_completed_counts_and_keeps_active() =
        runTest {
            dao.insert(TodoEntity("a", "Done 1", done = true, createdAt = 1))
            dao.insert(TodoEntity("b", "Done 2", done = true, createdAt = 2))
            dao.insert(TodoEntity("c", "Active", done = false, createdAt = 3))
            assertEquals(2, dao.deleteCompleted())
            assertEquals(listOf("c"), dao.observeAll().first().map { it.id })
        }
}
