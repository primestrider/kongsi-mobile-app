package com.primestrider.kotlinstarter.feature.example.products

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** API types stop at `data/`: screens and ViewModels only see the `model/` types. */
class ProductsArchitectureTest {
    @Test
    fun ui_never_sees_dto() {
        val ui = File("src/main/java/com/primestrider/kotlinstarter/feature/example/products/ui")
        assertTrue("missing ${ui.absolutePath}", ui.isDirectory && ui.walk().any { it.extension == "kt" })
        val leaks =
            ui
                .walk()
                .filter {
                    it.extension == "kt" &&
                        Regex(
                            """\w+Dto\b""",
                        ).containsMatchIn(it.readText())
                }.map { it.name }
                .toList()
        assertTrue("DTOs used in ui/: $leaks", leaks.isEmpty())
    }
}
