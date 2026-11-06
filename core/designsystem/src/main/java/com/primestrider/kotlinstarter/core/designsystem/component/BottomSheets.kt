package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import kotlinx.coroutines.launch

/**
 * Modal bottom sheet with an optional [title] and padded content. [dismissible] = false keeps it open
 * on back, swipe-down and outside taps; close it from your own content by not composing it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { dismissible || it != SheetValue.Hidden },
        )
    SheetFrame(onDismiss, modifier, title, dismissible, sheetState, content)
}

/** Shared sheet frame; [SheetState] stays internal so callers need no experimental opt-in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetFrame(
    onDismiss: () -> Unit,
    modifier: Modifier,
    title: String?,
    dismissible: Boolean,
    sheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        modifier = modifier,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = dismissible),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = Spacing.s4, end = Spacing.s4, bottom = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            content()
        }
    }
}

/** Single-choice list in a bottom sheet: a tap hides the sheet, then reports [onSelect] once, then [onDismiss]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionsBottomSheet(
    title: String,
    options: List<SelectOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var chosen by remember { mutableStateOf(false) }
    SheetFrame(onDismiss, modifier, title, dismissible = true, sheetState = sheetState) {
        Column(Modifier.selectableGroup()) {
            options.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = option.value == selected,
                            enabled = option.enabled,
                            role = Role.RadioButton,
                            onClick = {
                                if (chosen) return@selectable
                                chosen = true
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    onSelect(option.value)
                                    onDismiss()
                                }
                            },
                        ).heightIn(min = 48.dp)
                        .padding(vertical = Spacing.s3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                ) {
                    AppRadioButton(selected = option.value == selected, onClick = null, enabled = option.enabled)
                    Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AppBottomSheetPreview() {
    PreviewTheme {
        AppBottomSheet(onDismiss = {}, title = "Sheet title") {
            Text("Sheet content")
        }
    }
}

@ThemePreviews
@Composable
private fun OptionsBottomSheetPreview() {
    PreviewTheme {
        OptionsBottomSheet(
            title = "Pick one",
            options = listOf(SelectOption("a", "Option A"), SelectOption("b", "Option B"), SelectOption("c", "Disabled", enabled = false)),
            selected = "a",
            onSelect = {},
            onDismiss = {},
        )
    }
}
