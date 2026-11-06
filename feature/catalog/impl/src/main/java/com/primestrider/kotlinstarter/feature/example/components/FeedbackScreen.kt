package com.primestrider.kotlinstarter.feature.example.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.EmptyState
import com.primestrider.kotlinstarter.core.designsystem.component.ErrorState
import com.primestrider.kotlinstarter.core.designsystem.component.LoadingOverlay
import com.primestrider.kotlinstarter.core.designsystem.component.LoadingState
import com.primestrider.kotlinstarter.core.designsystem.component.LocalMessenger
import com.primestrider.kotlinstarter.core.designsystem.component.MessageVariant
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.component.UiMessage
import com.primestrider.kotlinstarter.core.designsystem.component.UiText
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.primestrider.kotlinstarter.core.designsystem.R as DesignR

private const val OVERLAY_DURATION_MS = 1500L

/** App-wide messages, loading / empty / error states and the loading overlay (banners live on the Display page). */
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    onSaveSucceeded: () -> Unit,
    onSaveFailed: () -> Unit,
) {
    ExamplePageScaffold(
        title = R.string.example_components_feedback_title,
        subtitle = R.string.example_components_feedback_subtitle,
        onBack = onBack,
    ) {
        MessagesSection(onSaveSucceeded, onSaveFailed)
        StateViewsSection()
        OverlaySection()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessagesSection(
    onSaveSucceeded: () -> Unit,
    onSaveFailed: () -> Unit,
) {
    val messenger = LocalMessenger.current
    val variants =
        listOf(
            Triple(R.string.components_snackbar_success, R.string.components_snackbar_success_message, MessageVariant.SUCCESS),
            Triple(R.string.components_snackbar_error, R.string.components_snackbar_error_message, MessageVariant.ERROR),
            Triple(R.string.components_snackbar_warning, R.string.components_snackbar_warning_message, MessageVariant.WARNING),
            Triple(R.string.components_snackbar_info, R.string.components_snackbar_info_message, MessageVariant.INFO),
        )
    DemoSection(R.string.feedback_section_messages) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            variants.forEach { (label, message, variant) ->
                AppButton(
                    stringResource(label),
                    onClick = { messenger.show(UiMessage(UiText.Resource(message), variant)) },
                    variant = ButtonVariant.OUTLINED,
                )
            }
            AppButton(
                stringResource(R.string.components_snackbar_delete_undo),
                onClick = {
                    messenger.show(
                        UiMessage(
                            text = UiText.Resource(R.string.components_snackbar_deleted),
                            actionLabel = UiText.Resource(R.string.components_snackbar_undo),
                            onAction = { messenger.success(UiText.Resource(R.string.components_snackbar_restored)) },
                        ),
                    )
                },
            )
            AppButton(stringResource(R.string.feedback_vm_success), onClick = onSaveSucceeded, variant = ButtonVariant.SECONDARY)
            AppButton(stringResource(R.string.feedback_vm_fail), onClick = onSaveFailed, variant = ButtonVariant.SECONDARY)
        }
    }
}

@Composable
private fun StateViewsSection() {
    val states =
        listOf(
            R.string.feedback_state_loading,
            R.string.feedback_state_empty,
            R.string.feedback_state_error,
            R.string.feedback_state_content,
        )
    var selected by rememberSaveable { mutableIntStateOf(0) }
    DemoSection(R.string.feedback_section_states) {
        val labels = states.map { stringResource(it) }
        SegmentedChoice(states.indices.toList(), selected, onSelect = { selected = it }, label = { labels[it] })
        Box(Modifier.fillMaxWidth().height(STATE_BOX_HEIGHT), contentAlignment = Alignment.Center) {
            when (selected) {
                0 -> LoadingState(message = stringResource(DesignR.string.component_loading))
                1 ->
                    EmptyState(
                        title = stringResource(R.string.components_empty_title),
                        description = stringResource(R.string.components_empty_description),
                        actionLabel = stringResource(R.string.components_empty_action),
                        onAction = { selected = 3 },
                    )
                2 -> ErrorState(message = stringResource(R.string.components_banner_error_body), onRetry = { selected = 0 })
                else -> Text(stringResource(R.string.feedback_content_text))
            }
        }
    }
}

private val STATE_BOX_HEIGHT = 280.dp

@Composable
private fun OverlaySection() {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    DemoSection(R.string.feedback_section_overlay) {
        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            AppButton(
                stringResource(R.string.feedback_overlay_show),
                onClick = {
                    visible = true
                    scope.launch {
                        delay(OVERLAY_DURATION_MS)
                        visible = false
                    }
                },
            )
            LoadingOverlay(visible = visible)
        }
    }
}

@ThemePreviews
@Composable
private fun FeedbackScreenPreview() {
    PreviewTheme {
        FeedbackScreen(onBack = {}, onSaveSucceeded = {}, onSaveFailed = {})
    }
}
