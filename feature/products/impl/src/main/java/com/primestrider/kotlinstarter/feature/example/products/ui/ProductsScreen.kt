package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppScaffold
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.EmptyState
import com.primestrider.kotlinstarter.core.designsystem.component.ErrorState
import com.primestrider.kotlinstarter.core.designsystem.component.SearchField
import com.primestrider.kotlinstarter.core.designsystem.component.Skeleton
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.network.asApiError
import com.primestrider.kotlinstarter.core.network.userMessage
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.core.util.formatCurrency
import com.primestrider.kotlinstarter.feature.example.products.model.Product
import com.primestrider.kotlinstarter.feature.example.products.model.Review
import com.primestrider.kotlinstarter.feature.products.impl.R
import kotlinx.coroutines.flow.flowOf
import com.primestrider.kotlinstarter.core.designsystem.R as DesignR

private const val SKELETON_ROWS = 6

/** Searchable, infinitely scrolling product list; every load state maps to a state component. */
@Composable
fun ProductsScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    products: LazyPagingItems<Product>,
    onOpen: (Long) -> Unit,
    onBack: () -> Unit,
) {
    AppScaffold(title = stringResource(R.string.products_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Text(
                    stringResource(R.string.products_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SearchField(query, onQueryChange, placeholder = stringResource(R.string.products_search_placeholder))
            }
            val refresh = products.loadState.refresh
            PullToRefreshBox(
                isRefreshing = refresh is LoadState.Loading && products.itemCount > 0,
                onRefresh = products::refresh,
                modifier = Modifier.fillMaxSize().padding(top = Spacing.s2),
            ) {
                when {
                    products.itemCount > 0 -> ProductList(products, onOpen)
                    refresh is LoadState.Loading -> Pullable { SkeletonList() }
                    refresh is LoadState.Error ->
                        Pullable {
                            ErrorState(
                                message = refresh.error.asApiError().userMessage(),
                                onRetry = products::retry,
                                title = stringResource(R.string.products_error_title),
                            )
                        }
                    else ->
                        Pullable {
                            EmptyState(
                                title = stringResource(R.string.products_empty_title),
                                description = stringResource(R.string.products_empty_description),
                            )
                        }
                }
            }
        }
    }
}

/** Fills the pull-to-refresh area with a scrollable container, so pulling works on the non-list states too. */
@Composable
private fun Pullable(content: @Composable () -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { Box(Modifier.fillParentMaxSize()) { content() } }
    }
}

/** An inline failure with a Retry button; [title] is set for a failed refresh shown above stale items. */
@Composable
private fun RetryRow(
    message: String,
    onRetry: () -> Unit,
    title: String? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error) }
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        AppButton(stringResource(DesignR.string.component_retry), onClick = onRetry, variant = ButtonVariant.TEXT)
    }
}

@Composable
private fun ProductList(
    products: LazyPagingItems<Product>,
    onOpen: (Long) -> Unit,
) {
    // A failed refresh (e.g. a new search while offline) keeps the previous items: say so above them. It sits outside
    // the list because an item inserted above the first visible row would be scrolled out of view.
    Column(Modifier.fillMaxSize()) {
        (products.loadState.refresh as? LoadState.Error)?.let { refresh ->
            RetryRow(
                message = refresh.error.asApiError().userMessage(),
                onRetry = products::retry,
                title = stringResource(R.string.products_error_title),
            )
        }
        ProductItems(products, onOpen)
    }
}

@Composable
private fun ColumnScope.ProductItems(
    products: LazyPagingItems<Product>,
    onOpen: (Long) -> Unit,
) {
    LazyColumn(Modifier.weight(1f)) {
        items(count = products.itemCount, key = products.itemKey { it.id }) { index ->
            products[index]?.let { ProductRow(it, onOpen) }
        }
        when (val append = products.loadState.append) {
            is LoadState.Loading ->
                item {
                    Box(Modifier.fillMaxWidth().padding(Spacing.s4), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            is LoadState.Error ->
                item { RetryRow(message = append.error.asApiError().userMessage(), onRetry = products::retry) }
            is LoadState.NotLoading -> Unit
        }
    }
}

@Composable
private fun ProductRow(
    product: Product,
    onOpen: (Long) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { onOpen(product.id) }
            .padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = product.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(Spacing.s16).clip(RoundedCornerShape(Radii.md)),
        )
        Column(Modifier.weight(1f)) {
            Text(product.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                product.brand ?: product.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(formatCurrency(product.price, locale = currentLocale()), style = MaterialTheme.typography.bodyMedium)
        }
        if (product.stock > 0) {
            Text(
                stringResource(R.string.products_stock, product.stock),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                stringResource(R.string.products_out_of_stock),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SkeletonList() {
    Column(Modifier.fillMaxSize()) {
        repeat(SKELETON_ROWS) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.s4, vertical = Spacing.s2),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Skeleton(Modifier.size(Spacing.s16), RoundedCornerShape(Radii.md))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1_5)) {
                    Skeleton(Modifier.fillMaxWidth(0.7f).height(Spacing.s4))
                    Skeleton(Modifier.fillMaxWidth(0.4f).height(Spacing.s3))
                    Skeleton(Modifier.fillMaxWidth(0.25f).height(Spacing.s3))
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ProductsScreenPreview() {
    val products =
        listOf(
            Product(
                id = 1,
                title = "Essence Mascara Lash Princess",
                description = "A popular mascara known for volumizing and lengthening effects.",
                category = "beauty",
                price = 9.99,
                discountPercentage = 7.17,
                rating = 4.9,
                stock = 5,
                brand = "Essence",
                thumbnail = "",
                images = emptyList(),
                reviews = listOf(Review(5, "Great product", "Eleanor Collins", "2025-04-30")),
            ),
            Product(
                id = 2,
                title = "Eyeshadow Palette with Mirror",
                description = "A versatile palette with a range of shades.",
                category = "beauty",
                price = 19.99,
                discountPercentage = 5.5,
                rating = 3.8,
                stock = 44,
                brand = "Glamour Beauty",
                thumbnail = "",
                images = emptyList(),
                reviews = emptyList(),
            ),
        )
    PreviewTheme {
        ProductsScreen(
            query = "",
            onQueryChange = {},
            products = flowOf(PagingData.from(products)).collectAsLazyPagingItems(),
            onOpen = {},
            onBack = {},
        )
    }
}
