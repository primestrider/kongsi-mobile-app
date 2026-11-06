package com.primestrider.kotlinstarter.feature.example.products.data

import com.primestrider.kotlinstarter.core.network.Anonymous
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class ProductDto(
    val id: Long,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val brand: String? = null,
    val thumbnail: String,
    val images: List<String> = emptyList(),
    val reviews: List<ReviewDto> = emptyList(),
)

@Serializable
data class ReviewDto(
    val rating: Int,
    val comment: String,
    val reviewerName: String,
    val date: String,
)

@Serializable
data class ProductPage(
    val products: List<ProductDto>,
    val total: Int,
    val skip: Int,
    val limit: Int,
)

/** DummyJSON's public product catalog; no bearer token needed. */
interface ProductsApi {
    @Anonymous
    @GET("products")
    suspend fun products(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int,
    ): ProductPage

    @Anonymous
    @GET("products/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("skip") skip: Int,
    ): ProductPage

    @Anonymous
    @GET("products/{id}")
    suspend fun product(
        @Path("id") id: Long,
    ): ProductDto
}
