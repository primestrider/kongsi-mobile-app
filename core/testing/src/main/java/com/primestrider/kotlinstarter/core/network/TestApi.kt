package com.primestrider.kotlinstarter.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class Item(
    val id: Int,
)

interface TestApi {
    @GET("item")
    suspend fun item(): Item

    @Anonymous
    @GET("open")
    suspend fun open(): Item
}
