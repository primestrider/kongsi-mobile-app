package com.primestrider.kotlinstarter.feature.example

import kotlinx.serialization.Serializable

// Routes of the products feature; the `feature.example` package keeps their serial names stable.

@Serializable
data object ProductsRoute

@Serializable
data class ProductDetailRoute(
    val id: Long,
)
