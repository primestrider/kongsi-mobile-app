package com.primestrider.kotlinstarter.feature.example.products.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.primestrider.kotlinstarter.core.network.ApiException
import com.primestrider.kotlinstarter.core.network.toApiError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

const val PAGE_SIZE = 20

/** Pages of products keyed by `skip`; a blank [query] lists everything, otherwise searches. */
class ProductsPagingSource(
    private val api: ProductsApi,
    private val query: String,
    private val json: Json,
) : PagingSource<Int, ProductDto>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ProductDto> {
        val skip = params.key ?: 0
        return try {
            val term = query.trim()
            val page =
                if (term.isEmpty()) api.products(params.loadSize, skip) else api.search(term, params.loadSize, skip)
            LoadResult.Page(
                data = page.products,
                prevKey = null,
                nextKey = (skip + page.products.size).takeIf { page.products.isNotEmpty() && it < page.total },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(ApiException(e.toApiError(json)))
        }
    }

    // Always restart from the top: a refresh after a search change must not land mid-list.
    override fun getRefreshKey(state: PagingState<Int, ProductDto>): Int? = null
}
