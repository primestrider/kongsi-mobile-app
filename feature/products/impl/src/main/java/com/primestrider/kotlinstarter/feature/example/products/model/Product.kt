package com.primestrider.kotlinstarter.feature.example.products.model

/** A product as the screens use it. The API's `ProductDto` is mapped to this in `data/`, so API changes stop there. */
data class Product(
    val id: Long,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val brand: String?,
    val thumbnail: String,
    val images: List<String>,
    val reviews: List<Review>,
)

data class Review(
    val rating: Int,
    val comment: String,
    val reviewerName: String,
    val date: String,
)
