package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.primestrider.kotlinstarter.feature.example.products.data.ProductsRepository
import com.primestrider.kotlinstarter.feature.example.products.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val QUERY_KEY = "query"
private const val SEARCH_DEBOUNCE_MILLIS = 350L

/** Search text (kept across process death) and the paged results for it. */
@HiltViewModel
class ProductsViewModel
    @Inject
    constructor(
        repository: ProductsRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val query: StateFlow<String> = savedStateHandle.getStateFlow(QUERY_KEY, "")

        fun onQueryChange(value: String) {
            savedStateHandle[QUERY_KEY] = value
        }

        // The first (restored or empty) query loads at once; typing waits for a pause.
        private val initialQuery = query.value.trim()

        @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
        val products: Flow<PagingData<Product>> =
            query
                .map { it.trim() }
                .debounce { if (it == initialQuery) 0L else SEARCH_DEBOUNCE_MILLIS }
                .distinctUntilChanged()
                .flatMapLatest(repository::pager)
                .cachedIn(viewModelScope)
    }
