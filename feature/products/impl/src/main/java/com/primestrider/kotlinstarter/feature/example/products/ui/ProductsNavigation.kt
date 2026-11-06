package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.primestrider.kotlinstarter.feature.example.ProductDetailRoute
import com.primestrider.kotlinstarter.feature.example.ProductsRoute

/** The products list (with the detail beside it on wide screens) and the detail page. */
fun NavGraphBuilder.productsDestinations(navController: NavController) {
    val back: () -> Unit = { navController.popBackStack() }
    composable<ProductsRoute> {
        val viewModel: ProductsViewModel = hiltViewModel()
        val query by viewModel.query.collectAsStateWithLifecycle()
        ProductsListDetail(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            products = viewModel.products.collectAsLazyPagingItems(),
            onOpenFullScreen = { id -> navController.navigate(ProductDetailRoute(id)) },
            onBack = back,
            detail = { id, onClose -> ProductDetail(id, onBack = onClose) },
        )
    }
    composable<ProductDetailRoute> { entry ->
        ProductDetail(entry.toRoute<ProductDetailRoute>().id, onBack = back)
    }
}

/** One product, with a ViewModel created for [id] (keyed, so each product in the pane gets its own). */
@Composable
private fun ProductDetail(
    id: Long,
    onBack: () -> Unit,
) {
    val viewModel =
        hiltViewModel<ProductDetailViewModel, ProductDetailViewModel.Factory>(key = "product-$id") { it.create(id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProductDetailScreen(state = state, onRetry = viewModel::retry, onAddToCart = viewModel::addToCart, onBack = onBack)
}
