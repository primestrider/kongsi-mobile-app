package com.primestrider.kotlinstarter.feature.example.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.rememberSaveableStateList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val LOADING_BUTTON_TAG = "actions_loading_button"
internal const val LOADING_DURATION_MS = 1500L

/** Material 3 buttons, icon buttons, FABs and chips, used directly. */
@Composable
fun ActionsScreen(onBack: () -> Unit) {
    ExamplePageScaffold(
        title = R.string.example_components_actions_title,
        subtitle = R.string.example_components_actions_subtitle,
        onBack = onBack,
    ) {
        ButtonsSection()
        LoadingButtonSection()
        IconButtonsSection()
        FabSection()
        ChipsSection()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ButtonsSection() {
    DemoSection(R.string.components_section_buttons) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            Button(onClick = {}) { Text(stringResource(R.string.components_button_filled)) }
            FilledTonalButton(onClick = {}) { Text(stringResource(R.string.components_button_tonal)) }
            OutlinedButton(onClick = {}) { Text(stringResource(R.string.components_button_outlined)) }
            TextButton(onClick = {}) { Text(stringResource(R.string.components_button_text)) }
            ElevatedButton(onClick = {}) { Text(stringResource(R.string.components_button_elevated)) }
            Button(
                onClick = {},
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) { Text(stringResource(R.string.components_button_destructive)) }
            Button(onClick = {}, enabled = false) { Text(stringResource(R.string.components_button_disabled)) }
            Button(onClick = {}, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.components_button_with_icon))
            }
        }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.components_button_full_width))
        }
    }
}

/** A button that runs a fake task: disabled with a spinner while busy, so a double tap runs it once. */
@Composable
private fun LoadingButtonSection() {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var runs by remember { mutableIntStateOf(0) }
    DemoSection(R.string.components_section_loading) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            // AppButton ignores clicks while loading, so a double tap runs the task once.
            AppButton(
                text = stringResource(R.string.components_loading_run),
                onClick = {
                    loading = true
                    scope.launch {
                        delay(LOADING_DURATION_MS)
                        runs++
                        loading = false
                    }
                },
                loading = loading,
                modifier = Modifier.testTag(LOADING_BUTTON_TAG),
            )
            Text(stringResource(R.string.components_loading_count, runs), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun IconButtonsSection() {
    var favorite by rememberSaveable { mutableStateOf(false) }
    DemoSection(R.string.components_section_icon_buttons) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            IconButton(onClick = {}) { Icon(Icons.Filled.Edit, stringResource(R.string.components_cd_edit)) }
            FilledIconButton(onClick = {}) { Icon(Icons.Filled.Add, stringResource(R.string.components_cd_add)) }
            FilledTonalIconButton(onClick = {}) { Icon(Icons.Filled.Edit, stringResource(R.string.components_cd_edit)) }
            OutlinedIconButton(onClick = {}) { Icon(Icons.Filled.Delete, stringResource(R.string.components_cd_delete)) }
            IconToggleButton(checked = favorite, onCheckedChange = { favorite = it }) {
                Icon(
                    if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.components_cd_favorite),
                )
            }
        }
    }
}

@Composable
private fun FabSection() {
    val add = stringResource(R.string.components_cd_add)
    DemoSection(R.string.components_section_fab) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            SmallFloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, add) }
            FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, add) }
            LargeFloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, add) }
        }
        ExtendedFloatingActionButton(
            onClick = {},
            icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
            text = { Text(stringResource(R.string.components_fab_extended)) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsSection() {
    var filterSelected by rememberSaveable { mutableStateOf(false) }
    val tags = rememberSaveableStateList("Kotlin", "Compose", "Android")
    DemoSection(R.string.components_section_chips) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            AssistChip(
                onClick = {},
                label = { Text(stringResource(R.string.components_chip_assist)) },
                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            )
            FilterChip(
                selected = filterSelected,
                onClick = { filterSelected = !filterSelected },
                label = { Text(stringResource(R.string.components_chip_filter)) },
            )
            SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.components_chip_suggestion)) })
            tags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(tag) },
                    trailingIcon = {
                        IconButton(onClick = { tags.remove(tag) }, modifier = Modifier.size(Spacing.s6)) {
                            Icon(Icons.Filled.Close, stringResource(R.string.components_cd_remove, tag))
                        }
                    },
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ActionsScreenPreview() {
    PreviewTheme {
        ActionsScreen(onBack = {})
    }
}
