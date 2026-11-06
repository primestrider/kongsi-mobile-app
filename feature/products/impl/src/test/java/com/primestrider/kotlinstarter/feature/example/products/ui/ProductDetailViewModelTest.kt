package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.component.MessageVariant
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.component.UiText
import com.primestrider.kotlinstarter.feature.products.impl.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

// toRoute() decodes through an android.os.Bundle, so this needs Robolectric.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {
    private val messenger = Messenger()
    private val created = mutableListOf<ProductDetailViewModel>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun viewModel(
        id: Long,
        repository: FakeProductsRepository,
    ) = ProductDetailViewModel(id, repository, Json, messenger).also { created += it }

    @Test
    fun non_positive_id_is_not_found_without_a_request() {
        val repository = FakeProductsRepository()
        assertEquals(ProductDetailState.NotFound, viewModel(0, repository).state.value)
        assertTrue(repository.requested.isEmpty())
    }

    @Test
    fun missing_product_is_not_found() {
        val repository =
            FakeProductsRepository(
                answer = { throw HttpException(Response.error<Any>(404, "{}".toResponseBody("application/json".toMediaType()))) },
            )
        assertEquals(ProductDetailState.NotFound, viewModel(7, repository).state.value)
    }

    @Test
    fun network_failure_then_retry_loads() {
        val repository = FakeProductsRepository(answer = { throw IOException("offline") })
        val viewModel = viewModel(7, repository)
        val failed = viewModel.state.value as ProductDetailState.Failed
        assertTrue(failed.error.isNetworkError)

        repository.answer = { product(it) }
        viewModel.retry()
        assertEquals(ProductDetailState.Loaded(product(7)), viewModel.state.value)
    }

    @Test
    fun add_to_cart_sends_one_success_message() =
        runTest {
            val viewModel = viewModel(7, FakeProductsRepository(answer = { product(it, stock = 5) }))
            viewModel.addToCart(99)
            val message = withTimeout(1_000) { messenger.messages.first() }
            assertEquals(MessageVariant.SUCCESS, message.variant)
            assertEquals(UiText.Resource(R.string.products_added_to_cart), message.text)
            assertNull(withTimeoutOrNull(100) { messenger.messages.first() })
        }
}
