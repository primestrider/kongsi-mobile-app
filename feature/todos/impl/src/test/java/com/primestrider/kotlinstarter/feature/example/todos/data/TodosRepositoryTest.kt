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

/** Every write refreshes the home-screen widget. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TodosRepositoryTest {
    private lateinit var database: ExampleDatabase
    private var updates = 0
    private lateinit var repository: DefaultTodosRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ExampleDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        var now = 0L
        repository = DefaultTodosRepository(database.todoDao(), { updates++ }) { ++now }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun widget_is_updated_after_every_write() =
        runTest {
            repository.add("Milk")
            val id =
                repository.todos
                    .first()
                    .single()
                    .id
            repository.toggle(id)
            repository.clearCompleted()
            repository.add("Bread")
            repository.remove(
                repository.todos
                    .first()
                    .single()
                    .id,
            )
            assertEquals(5, updates)
        }
}
