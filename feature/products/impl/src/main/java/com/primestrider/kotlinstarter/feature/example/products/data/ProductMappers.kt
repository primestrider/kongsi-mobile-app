package com.primestrider.kotlinstarter.feature.example.products.data

import com.primestrider.kotlinstarter.feature.example.products.model.Product
import com.primestrider.kotlinstarter.feature.example.products.model.Review

fun ProductDto.toProduct(): Product =
    Product(
        id = id,
        title = title,
        description = description,
        category = category,
        price = price,
        discountPercentage = discountPercentage,
        rating = rating,
        stock = stock,
        brand = brand,
        thumbnail = thumbnail,
        images = images,
        reviews = reviews.map { it.toReview() },
    )

fun ReviewDto.toReview(): Review = Review(rating = rating, comment = comment, reviewerName = reviewerName, date = date)

fun ProductDto.toEntity(position: Int): ProductEntity =
    ProductEntity(
        id = id,
        position = position,
        title = title,
        description = description,
        category = category,
        price = price,
        discountPercentage = discountPercentage,
        rating = rating,
        stock = stock,
        brand = brand,
        thumbnail = thumbnail,
        images = images,
        reviews = reviews,
    )

fun ProductEntity.toProduct(): Product =
    Product(
        id = id,
        title = title,
        description = description,
        category = category,
        price = price,
        discountPercentage = discountPercentage,
        rating = rating,
        stock = stock,
        brand = brand,
        thumbnail = thumbnail,
        images = images,
        reviews = reviews.map { it.toReview() },
    )
