package com.primestrider.kotlinstarter.feature.example.products.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.primestrider.kotlinstarter.core.network.ApiException
import com.primestrider.kotlinstarter.core.network.toApiError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

private const val ALL_PRODUCTS = "all"

/**
 * Fills [ProductsDatabase] from the network while the screen pages from the database only. A refresh replaces the
 * cache only once the first page has arrived, so when the network fails the cached rows stay on screen and the
 * error shows beside them.
 */
@OptIn(ExperimentalPagingApi::class)
class ProductsRemoteMediator(
    private val api: ProductsApi,
    private val database: ProductsDatabase,
    private val json: Json,
) : RemoteMediator<Int, ProductEntity>() {
    private val dao = database.productDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ProductEntity>,
    ): MediatorResult {
        val offset =
            when (loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND ->
                    dao.remoteKey(ALL_PRODUCTS)?.nextOffset ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        val limit = if (loadType == LoadType.REFRESH) state.config.initialLoadSize else state.config.pageSize
        return try {
            val page = api.products(limit, offset)
            val next = (offset + page.products.size).takeIf { page.products.isNotEmpty() && it < page.total }
            database.withTransaction {
                if (loadType == LoadType.REFRESH) dao.clearProducts()
                dao.insertAll(page.products.mapIndexed { index, dto -> dto.toEntity(position = offset + index) })
                dao.saveRemoteKey(RemoteKeyEntity(ALL_PRODUCTS, next))
            }
            MediatorResult.Success(endOfPaginationReached = next == null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(ApiException(e.toApiError(json)))
        }
    }
}
