package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.testing.resetAndroidUiDispatcher
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Wide windows show the list and the selected product side by side; phones keep opening the detail page. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ProductsListDetailTest {
    @get:Rule
    val rule = createComposeRule()

    private val opened = mutableListOf<Long>()

    @Before
    fun setUp() = resetAndroidUiDispatcher()

    private fun show(twoPanes: () -> Boolean? = { null }) =
        rule.setContent {
            val items = flowOf(PagingData.from(listOf(product(id = 7), product(id = 8)))).collectAsLazyPagingItems()
            AppTheme(false) {
                ProductsListDetail(
                    query = "",
                    onQueryChange = {},
                    products = items,
                    onOpenFullScreen = { opened += it },
                    onBack = {},
                    detail = { id, close -> Text("Detail of $id", Modifier.clickable(onClick = close)) },
                    twoPanes = twoPanes(),
                )
            }
        }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun wide_window_shows_the_selected_product_next_to_the_list() {
        show()
        rule.waitUntil(5_000) {
            rule
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText("Phone 7"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNodeWithText("Phone 7").performClick()
        rule.onNodeWithText("Detail of 7").assertExists()
        rule.onNodeWithText("Phone 8").assertExists()
        assertEquals(emptyList<Long>(), opened)
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun phone_opens_the_detail_page() {
        show()
        rule.waitUntil(5_000) {
            rule
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText("Phone 7"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNodeWithText("Phone 7").performClick()
        assertEquals(listOf(7L), opened)
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun closing_the_pane_shows_the_placeholder() {
        show()
        rule.waitUntil(5_000) {
            rule
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText("Phone 7"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNodeWithText("Phone 7").performClick()
        rule.onNodeWithText("Detail of 7").performClick()
        rule.onNodeWithText("Detail of 7").assertDoesNotExist()
        rule.onNodeWithText("Pick a product").assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun shrinking_to_one_pane_opens_the_selected_product() {
        var wide by mutableStateOf(true)
        show(twoPanes = { wide })
        rule.waitUntil(5_000) {
            rule
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText("Phone 7"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNodeWithText("Phone 7").performClick()
        rule.runOnIdle { wide = false }
        rule.waitForIdle()
        assertEquals(listOf(7L), opened)
        rule.runOnIdle { wide = true }
        rule.onNodeWithText("Pick a product").assertExists()
    }
}
