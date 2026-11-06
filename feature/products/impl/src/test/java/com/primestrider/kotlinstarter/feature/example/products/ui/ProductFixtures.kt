package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.paging.PagingData
import com.primestrider.kotlinstarter.feature.example.products.data.ProductsRepository
import com.primestrider.kotlinstarter.feature.example.products.model.Product
import com.primestrider.kotlinstarter.feature.example.products.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

fun product(
    id: Long = 1,
    stock: Int = 5,
    reviews: List<Review> = emptyList(),
) = Product(
    id = id,
    title = "Phone $id",
    description = "A phone.",
    category = "smartphones",
    price = 9.99,
    discountPercentage = 12.5,
    rating = 4.25,
    stock = stock,
    brand = null,
    thumbnail = "https://example.org/$id.png",
    images = emptyList(),
    reviews = reviews,
)

/** A repository whose [product] answers come from [answer] and that records the queries [pager] is asked for. */
class FakeProductsRepository(
    var answer: suspend (Long) -> Product = { product(it) },
) : ProductsRepository {
    val requested = mutableListOf<Long>()
    val queries = mutableListOf<String>()

    override fun pager(query: String): Flow<PagingData<Product>> {
        queries += query
        return flowOf(PagingData.empty())
    }

    override suspend fun product(id: Long): Product {
        requested += id
        return answer(id)
    }
}
