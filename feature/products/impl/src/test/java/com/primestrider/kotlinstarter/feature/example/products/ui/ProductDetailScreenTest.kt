package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.core.network.ApiError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ProductDetailScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var added: Int? = null

    private fun show(state: ProductDetailState) =
        rule.setContent {
            AppTheme(false) { ProductDetailScreen(state = state, onRetry = {}, onAddToCart = { added = it }, onBack = {}) }
        }

    @Test
    fun not_found_has_no_retry() {
        show(ProductDetailState.NotFound)
        rule.onNodeWithText("That product no longer exists.").assertExists()
        rule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun network_failure_offers_retry() {
        show(ProductDetailState.Failed(ApiError(null, isNetworkError = true)))
        rule.onNodeWithText("Cannot reach the server. Check your connection.").assertExists()
        rule.onNodeWithText("Retry").assertExists()
    }

    @Test
    fun loaded_shows_price_and_discount() {
        show(ProductDetailState.Loaded(product()))
        rule.onNodeWithText("$9.99").assertExists()
        rule.onNodeWithText("-12.5%").assertExists()
    }

    @Test
    fun reviews_tab_without_reviews() {
        show(ProductDetailState.Loaded(product()))
        rule.onNodeWithText("Reviews").performClick()
        rule.onNodeWithText("No reviews yet").assertExists()
    }

    @Test
    fun out_of_stock_cannot_be_added() {
        show(ProductDetailState.Loaded(product(stock = 0)))
        rule.onNodeWithText("Add to cart").assertIsNotEnabled()
    }

    @Test
    fun stepper_stays_between_one_and_stock() {
        show(ProductDetailState.Loaded(product(stock = 2)))
        rule.onNodeWithText("Add to cart").performClick()
        rule.onNodeWithContentDescription("Decrease quantity").performClick()
        rule.onNodeWithTag(QUANTITY_TAG).assertTextEquals("1")
        repeat(3) { rule.onNodeWithContentDescription("Increase quantity").performClick() }
        rule.onNodeWithTag(QUANTITY_TAG).assertTextEquals("2")
        rule.onNodeWithText("Confirm").performClick()
        assertEquals(2, added)
    }
}
