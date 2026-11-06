package com.primestrider.kotlinstarter.feature.example.products.data

import com.primestrider.kotlinstarter.feature.example.products.model.Product
import com.primestrider.kotlinstarter.feature.example.products.model.Review
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductMappersTest {
    @Test
    fun dto_maps_to_product() {
        val dto =
            ProductDto(
                id = 7,
                title = "Phone",
                description = "A phone.",
                category = "smartphones",
                price = 9.99,
                discountPercentage = 12.5,
                rating = 4.25,
                stock = 3,
                brand = null,
                thumbnail = "https://example.org/7.png",
                images = listOf("https://example.org/7a.png"),
                reviews = listOf(ReviewDto(rating = 5, comment = "Great", reviewerName = "Ann", date = "2024-05-23T08:56:21.618Z")),
            )
        assertEquals(
            Product(
                id = 7,
                title = "Phone",
                description = "A phone.",
                category = "smartphones",
                price = 9.99,
                discountPercentage = 12.5,
                rating = 4.25,
                stock = 3,
                brand = null,
                thumbnail = "https://example.org/7.png",
                images = listOf("https://example.org/7a.png"),
                reviews = listOf(Review(rating = 5, comment = "Great", reviewerName = "Ann", date = "2024-05-23T08:56:21.618Z")),
            ),
            dto.toProduct(),
        )
    }
}
