package com.primestrider.kotlinstarter.feature.example.products.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.primestrider.kotlinstarter.feature.example.products.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

/** Product data for the example screens, as [Product] models. */
interface ProductsRepository {
    /** Pages of products matching [query] (all products when blank). */
    fun pager(query: String): Flow<PagingData<Product>>

    suspend fun product(id: Long): Product
}

/**
 * [ProductsRepository] on DummyJSON, offline-first for the full list: it pages from [ProductsDatabase] while
 * [ProductsRemoteMediator] refreshes it, so the last list loaded still shows without a connection. Searches go
 * straight to the network ([ProductsPagingSource]); a product's detail falls back to its cached row when offline.
 */
class DefaultProductsRepository
    @Inject
    constructor(
        private val api: ProductsApi,
        private val database: ProductsDatabase,
        private val json: Json,
    ) : ProductsRepository {
        private val config = PagingConfig(pageSize = PAGE_SIZE, initialLoadSize = PAGE_SIZE, enablePlaceholders = false)

        @OptIn(ExperimentalPagingApi::class)
        override fun pager(query: String): Flow<PagingData<Product>> =
            if (query.isBlank()) {
                Pager(config, remoteMediator = ProductsRemoteMediator(api, database, json)) {
                    database.productDao().pagingSource()
                }.flow.map { page -> page.map { it.toProduct() } }
            } else {
                Pager(config) { ProductsPagingSource(api, query, json) }.flow.map { page -> page.map { it.toProduct() } }
            }

        override suspend fun product(id: Long): Product =
            try {
                api.product(id).toProduct()
            } catch (e: IOException) {
                database.productDao().product(id)?.toProduct() ?: throw e
            }
    }
