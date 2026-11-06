package com.primestrider.kotlinstarter.feature.example.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import kotlinx.coroutines.launch
import com.primestrider.kotlinstarter.core.designsystem.R as DesignR

/**
 * A component that needs the whole screen (its own scaffold, scroll or drawer), opened from the App bars page
 * through `ComponentDemoRoute(id)`. [usage] and [api] label it on that page.
 */
data class ComponentDemo(
    val id: String,
    @StringRes val title: Int,
    @StringRes val usage: Int,
    val api: String,
    val content: @Composable (onBack: () -> Unit) -> Unit,
)

private enum class BarKind { Small, Center, Medium, Large }

/** The full-screen demos, in the order the App bars page lists them. */
val componentDemos: List<ComponentDemo> =
    listOf(
        ComponentDemo(
            "top-app-bar-small",
            R.string.components_demo_small_bar,
            R.string.components_demo_small_bar_usage,
            "TopAppBar + pinnedScrollBehavior",
        ) {
            AppBarDemo(BarKind.Small, it)
        },
        ComponentDemo(
            "top-app-bar-center",
            R.string.components_demo_center_bar,
            R.string.components_demo_center_bar_usage,
            "CenterAlignedTopAppBar + enterAlwaysScrollBehavior",
        ) { AppBarDemo(BarKind.Center, it) },
        ComponentDemo(
            "top-app-bar-medium",
            R.string.components_demo_medium_bar,
            R.string.components_demo_medium_bar_usage,
            "MediumTopAppBar + exitUntilCollapsedScrollBehavior",
        ) { AppBarDemo(BarKind.Medium, it) },
        ComponentDemo(
            "top-app-bar-large",
            R.string.components_demo_large_bar,
            R.string.components_demo_large_bar_usage,
            "LargeTopAppBar + exitUntilCollapsedScrollBehavior",
        ) { AppBarDemo(BarKind.Large, it) },
        ComponentDemo(
            "bottom-app-bar",
            R.string.components_demo_bottom_bar,
            R.string.components_demo_bottom_bar_usage,
            "BottomAppBar(actions, floatingActionButton)",
        ) {
            BottomAppBarDemo(it)
        },
        ComponentDemo(
            "modal-drawer",
            R.string.components_demo_modal_drawer,
            R.string.components_demo_modal_drawer_usage,
            "ModalNavigationDrawer + ModalDrawerSheet",
        ) {
            DrawerDemo(modal = true, onBack = it)
        },
        ComponentDemo(
            "dismissible-drawer",
            R.string.components_demo_dismissible_drawer,
            R.string.components_demo_dismissible_drawer_usage,
            "DismissibleNavigationDrawer + DismissibleDrawerSheet",
        ) { DrawerDemo(modal = false, onBack = it) },
        ComponentDemo(
            "search-bar",
            R.string.components_demo_search,
            R.string.components_demo_search_usage,
            "SearchBar + ExpandedFullScreenSearchBar",
        ) {
            SearchBarDemo(docked = false, onBack = it)
        },
        ComponentDemo(
            "docked-search-bar",
            R.string.components_demo_docked_search,
            R.string.components_demo_docked_search_usage,
            "SearchBar + ExpandedDockedSearchBar",
        ) {
            SearchBarDemo(docked = true, onBack = it)
        },
        ComponentDemo(
            "bottom-sheet-scaffold",
            R.string.components_demo_sheet_scaffold,
            R.string.components_demo_sheet_scaffold_usage,
            "BottomSheetScaffold",
        ) {
            BottomSheetScaffoldDemo(it)
        },
    )

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(DesignR.string.component_back))
    }
}

/** Enough rows to scroll, so scroll behaviours have something to react to. */
@Composable
private fun DemoRows(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().testTag("demoList"), contentPadding = padding) {
        items(40) { index ->
            ListItem(headlineContent = { Text(stringResource(R.string.components_demo_row, index + 1)) })
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBarDemo(
    kind: BarKind,
    onBack: () -> Unit,
) {
    val barState = rememberTopAppBarState()
    val behavior: TopAppBarScrollBehavior =
        when (kind) {
            BarKind.Small -> TopAppBarDefaults.pinnedScrollBehavior(barState)
            BarKind.Center -> TopAppBarDefaults.enterAlwaysScrollBehavior(barState)
            BarKind.Medium, BarKind.Large -> TopAppBarDefaults.exitUntilCollapsedScrollBehavior(barState)
        }
    val title: @Composable () -> Unit = { Text(stringResource(R.string.components_demo_bar_title)) }
    val back: @Composable () -> Unit = { BackButton(onBack) }
    val actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        IconButton(onClick = {}) { Icon(Icons.Filled.Search, stringResource(R.string.components_demo_search_action)) }
    }
    val barModifier = Modifier.testTag("topAppBar")
    Scaffold(
        modifier = Modifier.nestedScroll(behavior.nestedScrollConnection),
        topBar = {
            when (kind) {
                BarKind.Small -> TopAppBar(title, barModifier, back, actions, scrollBehavior = behavior)
                BarKind.Center -> CenterAlignedTopAppBar(title, barModifier, back, actions, scrollBehavior = behavior)
                BarKind.Medium -> MediumTopAppBar(title, barModifier, back, actions, scrollBehavior = behavior)
                BarKind.Large -> LargeTopAppBar(title, barModifier, back, actions, scrollBehavior = behavior)
            }
        },
    ) { padding -> DemoRows(padding) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomAppBarDemo(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.components_demo_bottom_bar)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Filled.Search, stringResource(R.string.components_demo_search_action)) }
                    IconButton(onClick = {}) { Icon(Icons.Filled.Favorite, stringResource(R.string.components_demo_favourites)) }
                    IconButton(onClick = {}) { Icon(Icons.Filled.Share, stringResource(R.string.components_demo_share)) }
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, stringResource(R.string.components_demo_add)) }
                },
            )
        },
    ) { padding -> DemoRows(padding) }
}

private data class Destination(
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val destinations =
    listOf(
        Destination(R.string.components_demo_inbox, Icons.Filled.Email),
        Destination(R.string.components_demo_outbox, Icons.Filled.Send),
        Destination(R.string.components_demo_favourites, Icons.Filled.Favorite),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerDemo(
    modal: Boolean,
    onBack: () -> Unit,
) {
    val drawer: DrawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val items: @Composable () -> Unit = {
        Text(
            stringResource(R.string.components_demo_drawer_header),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(Spacing.s4),
        )
        destinations.forEachIndexed { index, destination ->
            NavigationDrawerItem(
                label = { Text(stringResource(destination.label)) },
                icon = { Icon(destination.icon, contentDescription = null) },
                selected = index == selected,
                onClick = {
                    selected = index
                    if (modal) scope.launch { drawer.close() }
                },
                modifier = Modifier.padding(horizontal = Spacing.s3),
            )
        }
    }
    val content: @Composable () -> Unit = {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                if (modal) R.string.components_demo_modal_drawer else R.string.components_demo_dismissible_drawer,
                            ),
                        )
                    },
                    navigationIcon = {
                        val open = drawer.isOpen
                        IconButton(onClick = { scope.launch { if (open) drawer.close() else drawer.open() } }) {
                            Icon(
                                if (open) Icons.Filled.Close else Icons.Filled.Menu,
                                stringResource(if (open) R.string.components_demo_close_drawer else R.string.components_demo_open_drawer),
                            )
                        }
                    },
                    actions = { BackButton(onBack) },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.components_demo_drawer_selected, stringResource(destinations[selected].label)))
            }
        }
    }
    if (modal) {
        ModalNavigationDrawer(drawerContent = { ModalDrawerSheet { items() } }, drawerState = drawer, content = content)
    } else {
        DismissibleNavigationDrawer(drawerContent = { DismissibleDrawerSheet { items() } }, drawerState = drawer, content = content)
    }
}

private val fruits =
    listOf("Apple", "Apricot", "Banana", "Blueberry", "Cherry", "Grape", "Mango", "Orange", "Pear", "Pineapple")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarDemo(
    docked: Boolean,
    onBack: () -> Unit,
) {
    val searchState = rememberSearchBarState()
    val text = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    val query = text.text.toString()
    val matches = fruits.filter { query.isBlank() || it.contains(query, ignoreCase = true) }

    @Composable
    fun input(tag: String) {
        SearchBarDefaults.InputField(
            textFieldState = text,
            searchBarState = searchState,
            onSearch = { scope.launch { searchState.animateToCollapsed() } },
            placeholder = { Text(stringResource(R.string.components_demo_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.testTag(tag),
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (docked) R.string.components_demo_docked_search else R.string.components_demo_search)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.s4)) {
            SearchBar(state = searchState, inputField = { input("searchInput") }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.components_demo_search_result, query.ifBlank { "—" }),
                modifier = Modifier.padding(vertical = Spacing.s4),
            )
        }
        // A suggestion fills the query and closes the expanded bar, like a search submitted by hand.
        val suggestions: @Composable ColumnScope.() -> Unit = {
            matches.forEach { fruit ->
                ListItem(
                    headlineContent = { Text(fruit) },
                    modifier =
                        Modifier.clickable {
                            text.setTextAndPlaceCursorAtEnd(fruit)
                            scope.launch { searchState.animateToCollapsed() }
                        },
                )
            }
        }
        if (docked) {
            ExpandedDockedSearchBar(state = searchState, inputField = { input("searchInputExpanded") }, content = suggestions)
        } else {
            ExpandedFullScreenSearchBar(state = searchState, inputField = { input("searchInputExpanded") }, content = suggestions)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetScaffoldDemo(onBack: () -> Unit) {
    val state = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    BottomSheetScaffold(
        scaffoldState = state,
        sheetPeekHeight = 128.dp,
        sheetDragHandle = null,
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                Column(Modifier.fillMaxWidth().testTag("sheetHandle"), horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.BottomSheetDefaults
                        .DragHandle()
                    Text(stringResource(R.string.components_demo_sheet_peek), style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(R.string.components_demo_sheet_body), modifier = Modifier.padding(top = 160.dp))
                Text(stringResource(R.string.components_demo_sheet_detail))
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.components_demo_sheet_scaffold)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            Text(stringResource(R.string.components_demo_sheet_page))
            AppButton(
                text = stringResource(R.string.components_demo_sheet_expand),
                onClick = { scope.launch { state.bottomSheetState.expand() } },
                variant = ButtonVariant.SECONDARY,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ComponentDemosPreview() {
    PreviewTheme {
        componentDemos.first().content {}
    }
}
