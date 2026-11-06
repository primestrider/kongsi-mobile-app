package com.primestrider.kotlinstarter.feature.example.todos.widget

import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TodosWidgetTest {
    @Test
    fun widget_shows_open_count() =
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            provideComposable { TodosWidgetContent(TodosWidgetState(openCount = 4, newest = listOf("Milk", "Bread", "Eggs"))) }
            onNode(hasText("4 open")).assertExists()
            onNode(hasText("Milk")).assertExists()
            onNode(hasText("Eggs")).assertExists()
        }

    @Test
    fun empty_widget_says_nothing_to_do() =
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            provideComposable { TodosWidgetContent(TodosWidgetState(openCount = 0, newest = emptyList())) }
            onNode(hasText("Nothing to do")).assertExists()
        }

    @Test
    fun state_counts_open_todos_and_keeps_the_three_newest() {
        val todos =
            listOf("a" to false, "b" to true, "c" to false, "d" to false, "e" to false).mapIndexed { i, (title, done) ->
                com.primestrider.kotlinstarter.feature.example.todos.model
                    .Todo(id = "$i", title = title, done = done, createdAt = 10L - i)
            }
        org.junit.Assert.assertEquals(TodosWidgetState(openCount = 4, newest = listOf("a", "c", "d")), TodosWidgetState.from(todos))
    }
}
