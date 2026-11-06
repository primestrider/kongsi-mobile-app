package com.primestrider.kotlinstarter.feature.example.todos

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Room types stop at `data/`: screens and ViewModels only see the `model/` types. */
class TodosArchitectureTest {
    @Test
    fun ui_never_sees_entity() {
        val ui = File("src/main/java/com/primestrider/kotlinstarter/feature/example/todos/ui")
        assertTrue("missing ${ui.absolutePath}", ui.isDirectory && ui.walk().any { it.extension == "kt" })
        val leaks =
            ui
                .walk()
                .filter {
                    it.extension == "kt" && Regex("""\w+(Entity|Dao)\b""").containsMatchIn(it.readText())
                }.map { it.name }
                .toList()
        assertTrue("Room types used in ui/: $leaks", leaks.isEmpty())
    }
}
