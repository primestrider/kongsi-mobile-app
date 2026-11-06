package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.feature.example.products.model.Product
import com.primestrider.kotlinstarter.testing.resetAndroidUiDispatcher
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ProductsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() = resetAndroidUiDispatcher()

    private val done = LoadState.NotLoading(endOfPaginationReached = false)

    private fun show(
        items: List<Product>,
        states: LoadStates,
        onOpen: (Long) -> Unit = {},
    ) = rule.setContent {
        val paging = flowOf(PagingData.from(items, states)).collectAsLazyPagingItems()
        AppTheme(false) {
            Surface { ProductsScreen(query = "", onQueryChange = {}, products = paging, onOpen = onOpen, onBack = {}) }
        }
    }

    // LazyPagingItems receives PagingData on its own dispatcher, which compose idling does not track.
    private fun waitFor(text: String) = rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun append_error_keeps_items_and_offers_retry() {
        show(listOf(product(1, stock = 0)), LoadStates(refresh = done, prepend = done, append = LoadState.Error(IOException())))
        waitFor("Phone 1")
        waitFor("Retry")
    }

    @Test
    fun empty_result_shows_empty_state() {
        show(emptyList(), LoadStates(refresh = done, prepend = done, append = done))
        waitFor("No products found")
    }

    @Test
    fun refresh_error_without_items_shows_error_state() {
        show(emptyList(), LoadStates(refresh = LoadState.Error(IOException()), prepend = done, append = done))
        waitFor("Could not load products")
        waitFor("Retry")
    }

    @Test
    fun out_of_stock_is_labelled() {
        show(listOf(product(1, stock = 0)), LoadStates(refresh = done, prepend = done, append = done))
        waitFor("Out of stock")
    }

    @Test
    fun clicking_a_row_opens_it() {
        var opened: Long? = null
        show(listOf(product(7, stock = 0)), LoadStates(refresh = done, prepend = done, append = done), onOpen = { opened = it })
        waitFor("Phone 7")
        rule.onNodeWithText("Phone 7").performClick()
        assertEquals(7L, opened)
    }

    @Test
    fun refresh_error_with_items_keeps_them_and_offers_retry() {
        show(listOf(product(1, stock = 0)), LoadStates(refresh = LoadState.Error(IOException()), prepend = done, append = done))
        waitFor("Phone 1")
        waitFor("Could not load products")
        waitFor("Retry")
    }
}
