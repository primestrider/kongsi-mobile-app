package com.primestrider.kotlinstarter.feature.example.products.data

import java.io.IOException

fun productDto(id: Long) =
    ProductDto(
        id = id,
        title = "Phone $id",
        description = "A phone.",
        category = "smartphones",
        price = 9.99,
        discountPercentage = 0.0,
        rating = 4.0,
        stock = 3,
        thumbnail = "https://example.org/$id.png",
        images = listOf("https://example.org/$id-1.png"),
        reviews = listOf(ReviewDto(5, "Great", "Ana", "2026-01-01")),
    )

/** A catalog of [total] products served from memory; set [offline] to make every call fail like a lost connection. */
class FakeProductsApi(
    private val total: Int = 50,
) : ProductsApi {
    var offline = false
    val calls = mutableListOf<String>()

    private fun check() {
        if (offline) throw IOException("offline")
    }

    override suspend fun products(
        limit: Int,
        skip: Int,
    ): ProductPage {
        calls += "products($limit,$skip)"
        check()
        val items = (skip until minOf(skip + limit, total)).map { productDto(it + 1L) }
        return ProductPage(items, total, skip, limit)
    }

    override suspend fun search(
        query: String,
        limit: Int,
        skip: Int,
    ): ProductPage {
        calls += "search($query)"
        check()
        return ProductPage(emptyList(), 0, skip, limit)
    }

    override suspend fun product(id: Long): ProductDto {
        calls += "product($id)"
        check()
        return productDto(id)
    }
}
