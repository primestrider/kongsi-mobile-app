package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.primestrider.kotlinstarter.core.designsystem.component.AppBottomSheet
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppScaffold
import com.primestrider.kotlinstarter.core.designsystem.component.EmptyState
import com.primestrider.kotlinstarter.core.designsystem.component.ErrorState
import com.primestrider.kotlinstarter.core.designsystem.component.InfoRow
import com.primestrider.kotlinstarter.core.designsystem.component.LoadingState
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.network.userMessage
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.core.util.formatCurrency
import com.primestrider.kotlinstarter.core.util.formatIsoDate
import com.primestrider.kotlinstarter.core.util.formatPercent
import com.primestrider.kotlinstarter.feature.example.products.model.Product
import com.primestrider.kotlinstarter.feature.example.products.model.Review
import com.primestrider.kotlinstarter.feature.products.impl.R
import java.util.Locale

/** Tag of the quantity value in the add-to-cart sheet. */
const val QUANTITY_TAG = "product_quantity"

private enum class DetailTab(
    val label: Int,
) {
    OVERVIEW(R.string.products_tab_overview),
    SPECS(R.string.products_tab_specs),
    REVIEWS(R.string.products_tab_reviews),
}

/** One product: image, price, Overview / Specs / Reviews tabs and an add-to-cart sheet. */
@Composable
fun ProductDetailScreen(
    state: ProductDetailState,
    onRetry: () -> Unit,
    onAddToCart: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val title =
        when (state) {
            is ProductDetailState.Loaded -> state.product.title
            ProductDetailState.Loading -> stringResource(R.string.products_loading)
            else -> stringResource(R.string.products_title)
        }
    AppScaffold(title = title, onBack = onBack) { padding ->
        val content = Modifier.fillMaxSize().padding(padding)
        when (state) {
            ProductDetailState.Loading -> LoadingState(content)
            ProductDetailState.NotFound -> EmptyState(stringResource(R.string.products_not_found), content)
            is ProductDetailState.Failed -> ErrorState(state.error.userMessage(), onRetry, content)
            is ProductDetailState.Loaded -> ProductDetail(state.product, onAddToCart, content)
        }
    }
}

@Composable
private fun ProductDetail(
    product: Product,
    onAddToCart: (Int) -> Unit,
    modifier: Modifier,
) {
    val locale = currentLocale()
    var tab by rememberSaveable { mutableStateOf(DetailTab.OVERVIEW) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            AsyncImage(
                model = product.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(Radii.lg)),
            )
            Text(product.title, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
                Text(formatCurrency(product.price, locale = locale), style = MaterialTheme.typography.titleMedium)
                if (product.discountPercentage > 0) {
                    Badge { Text("-" + formatPercent(product.discountPercentage / 100, locale)) }
                }
            }
            PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                DetailTab.entries.forEach { entry ->
                    Tab(selected = tab == entry, onClick = { tab = entry }, text = { Text(stringResource(entry.label)) })
                }
            }
            when (tab) {
                DetailTab.OVERVIEW -> Text(product.description, style = MaterialTheme.typography.bodyMedium)
                DetailTab.SPECS -> Specs(product, locale)
                DetailTab.REVIEWS -> Reviews(product.reviews)
            }
        }
        AppButton(
            text = stringResource(R.string.products_add_to_cart),
            onClick = { sheetOpen = true },
            enabled = product.stock > 0,
            fullWidth = true,
            modifier = Modifier.padding(Spacing.s4),
        )
    }
    if (sheetOpen) {
        AddToCartSheet(
            stock = product.stock,
            onConfirm = { quantity ->
                sheetOpen = false
                onAddToCart(quantity)
            },
            onDismiss = { sheetOpen = false },
        )
    }
}

@Composable
private fun Specs(
    product: Product,
    locale: Locale,
) {
    Column {
        InfoRow(stringResource(R.string.products_brand), product.brand ?: "-")
        InfoRow(stringResource(R.string.products_category), product.category)
        InfoRow(stringResource(R.string.products_rating), String.format(locale, "%.1f", product.rating))
        InfoRow(stringResource(R.string.products_stock_label), product.stock.toString())
    }
}

@Composable
private fun Reviews(reviews: List<Review>) {
    if (reviews.isEmpty()) {
        Text(
            stringResource(R.string.products_no_reviews),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        reviews.forEachIndexed { index, review ->
            if (index > 0) HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
                    Text(review.reviewerName, style = MaterialTheme.typography.titleSmall)
                    Badge { Text("${review.rating}/5") }
                }
                Text(review.comment, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatIsoDate(review.date, currentLocale()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddToCartSheet(
    stock: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.products_add_to_cart)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Text(stringResource(R.string.products_quantity), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { quantity = (quantity - 1).coerceAtLeast(1) }, enabled = quantity > 1) {
                Icon(MinusIcon, contentDescription = stringResource(R.string.products_cd_decrease))
            }
            Text(
                quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(QUANTITY_TAG).semantics { liveRegion = LiveRegionMode.Polite },
            )
            IconButton(onClick = { quantity = (quantity + 1).coerceAtMost(stock) }, enabled = quantity < stock) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.products_cd_increase))
            }
        }
        AppButton(stringResource(R.string.products_confirm), onClick = { onConfirm(quantity) }, fullWidth = true)
    }
}

// material-icons-core has no "Remove"; this is the same 24dp minus bar.
private val MinusIcon: ImageVector =
    ImageVector
        .Builder(name = "Minus", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 13f)
            horizontalLineTo(5f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(14f)
            close()
        }.build()

@ThemePreviews
@Composable
private fun ProductDetailScreenPreview() {
    PreviewTheme {
        ProductDetailScreen(
            state =
                ProductDetailState.Loaded(
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
                ),
            onRetry = {},
            onAddToCart = {},
            onBack = {},
        )
    }
}

@ThemePreviews
@Composable
private fun ProductDetailScreenLoadingPreview() {
    PreviewTheme {
        ProductDetailScreen(state = ProductDetailState.Loading, onRetry = {}, onAddToCart = {}, onBack = {})
    }
}
