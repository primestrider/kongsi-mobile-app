package com.primestrider.kotlinstarter.feature.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.rememberSaveableStateList
import kotlinx.coroutines.delay

// Gesture-driven components of the Display page, kept out of DisplayScreen.kt so each stays focused.

private const val FILE = "RefreshDismissSections.kt"

/** How long the fake refresh takes. */
private const val REFRESH_MILLIS = 1_500L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PullToRefreshSection() {
    var refreshing by rememberSaveable { mutableStateOf(false) }
    var count by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            delay(REFRESH_MILLIS)
            count++
            refreshing = false
        }
    }
    DemoSection(
        R.string.components_section_pull_refresh,
        R.string.components_pull_refresh_usage,
        "PullToRefreshBox(isRefreshing, onRefresh)",
        FILE,
    ) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { refreshing = true },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            LazyColumn(Modifier.fillMaxSize().testTag("pullRefreshList")) {
                items(12) { index ->
                    ListItem(headlineContent = { Text(stringResource(R.string.components_pull_refresh_row, index + 1, count)) })
                    HorizontalDivider()
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.CenterVertically) {
            AppButton(
                text = stringResource(R.string.components_pull_refresh_button),
                onClick = { refreshing = true },
                variant = ButtonVariant.SECONDARY,
                loading = refreshing,
            )
            Text(stringResource(R.string.components_pull_refresh_count, count), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun SwipeToDismissSection() {
    val messages = rememberSaveableStateList(1, 2, 3, 4)
    // The last removed message and where it was, so Undo can put it back; -1 = nothing to undo.
    var removed by rememberSaveable { mutableIntStateOf(-1) }
    var removedAt by rememberSaveable { mutableIntStateOf(0) }

    fun remove(id: Int) {
        val index = messages.indexOf(id)
        if (index < 0) return
        messages.removeAt(index)
        removed = id
        removedAt = index
    }
    DemoSection(
        R.string.components_section_swipe_dismiss,
        R.string.components_swipe_dismiss_usage,
        "SwipeToDismissBox(rememberSwipeToDismissBoxState())",
        FILE,
    ) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
            messages.forEach { id ->
                // A fresh state per message: a recycled one would still read "dismissed" for the next row.
                key(id) {
                    val label = stringResource(R.string.components_swipe_dismiss_row, id)
                    val state = rememberSwipeToDismissBoxState()
                    SwipeToDismissBox(
                        state = state,
                        backgroundContent = {
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                    ).padding(horizontal = Spacing.s4),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        onDismiss = { remove(id) },
                        modifier = Modifier.testTag("dismissRow-$id"),
                    ) {
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = {
                                IconButton(onClick = { remove(id) }) {
                                    Icon(Icons.Filled.Delete, stringResource(R.string.components_swipe_dismiss_delete, label))
                                }
                            },
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
        if (removed >= 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        R.string.components_swipe_dismiss_removed,
                        stringResource(R.string.components_swipe_dismiss_row, removed),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    messages.add(removedAt.coerceIn(0, messages.size), removed)
                    removed = -1
                }) { Text(stringResource(R.string.components_swipe_dismiss_undo)) }
            }
        }
    }
}
