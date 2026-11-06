package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.paging.compose.LazyPagingItems
import com.primestrider.kotlinstarter.core.designsystem.component.EmptyState
import com.primestrider.kotlinstarter.feature.example.products.model.Product
import com.primestrider.kotlinstarter.feature.products.impl.R

/**
 * The products list, with the selected product beside it when the window is wide enough for two panes (tablets,
 * foldables, desktop windows). On narrower windows it is just [ProductsScreen] and a tap calls [onOpenFullScreen], which
 * opens the detail page as before; if the window shrinks while a product is selected, that product opens as a page.
 * [detail] draws one product (with a close action); the caller supplies it so this stays free of ViewModels.
 * [twoPanes] overrides the window's answer, for previews and tests.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ProductsListDetail(
    query: String,
    onQueryChange: (String) -> Unit,
    products: LazyPagingItems<Product>,
    onOpenFullScreen: (Long) -> Unit,
    onBack: () -> Unit,
    detail: @Composable (id: Long, onClose: () -> Unit) -> Unit,
    twoPanes: Boolean? = null,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val wide = twoPanes ?: (navigator.scaffoldDirective.maxHorizontalPartitions >= 2)
    // With two panes both are always shown, so the selection is plain state (no pane history to go back through).
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    val openFullScreen by rememberUpdatedState(onOpenFullScreen)

    LaunchedEffect(wide) {
        val id = selected
        if (!wide && id != null) {
            selected = null
            openFullScreen(id)
        }
    }

    if (!wide) {
        ProductsScreen(query = query, onQueryChange = onQueryChange, products = products, onOpen = onOpenFullScreen, onBack = onBack)
        return
    }
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                ProductsScreen(
                    query = query,
                    onQueryChange = onQueryChange,
                    products = products,
                    onOpen = { selected = it },
                    onBack = onBack,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val id = selected
                if (id == null) {
                    EmptyState(
                        title = stringResource(R.string.products_pick_title),
                        description = stringResource(R.string.products_pick_description),
                        icon = Icons.AutoMirrored.Filled.List,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // Each product gets its own ViewModels and scroll state, dropped when another one is picked.
                    key(id) { ScopedViewModelStore(key = id) { detail(id) { selected = null } } }
                }
            }
        },
    )
}

/**
 * Gives [content] its own ViewModelStore, cleared when [key] changes or [content] leaves the composition, so the
 * ViewModels of a product shown in a pane do not pile up in the screen's back stack entry. Creation still goes
 * through the parent owner's factory (Hilt's, inside a NavHost).
 */
@Composable
internal fun ScopedViewModelStore(
    key: Any,
    content: @Composable () -> Unit,
) {
    val parent = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner" }
    val owner =
        remember(key) {
            object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
                override val viewModelStore = ViewModelStore()
                override val defaultViewModelProviderFactory: ViewModelProvider.Factory
                    get() =
                        (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelProviderFactory
                            ?: ViewModelProvider.NewInstanceFactory()
                override val defaultViewModelCreationExtras: CreationExtras
                    get() = (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty
            }
        }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
