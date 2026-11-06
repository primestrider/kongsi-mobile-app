package com.primestrider.kotlinstarter.feature.example.animation.transitions

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.feature.animation.impl.R
import com.primestrider.kotlinstarter.feature.example.animation.AnimExample
import com.primestrider.kotlinstarter.feature.example.animation.MinTouchTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Scale of the preview panel at the end of the back gesture. */
private const val PANEL_MIN_SCALE = 0.9f

/** How far the panel drifts towards the swipe at the end of the back gesture. */
private val PanelShift = 24.dp

/**
 * A panel that follows the predictive back gesture through [PredictiveBackHandler]: it shrinks and drifts with the
 * finger, closes when the gesture commits and springs back when it is cancelled. Below Android 14 there is no
 * gesture progress, so the same handler simply closes the panel on back and a note says why.
 */
@Composable
internal fun PredictiveBackDemo() {
    var open by rememberSaveable { mutableStateOf(false) }
    AnimExample(
        R.string.anim_transitions_predictive_title,
        R.string.anim_transitions_predictive_usage,
        "PredictiveBackHandler",
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Text(
                stringResource(R.string.anim_transitions_predictive_fallback),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Card(
            onClick = { open = true },
            enabled = !open,
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
        ) {
            Text(
                stringResource(R.string.anim_transitions_open_preview),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(Spacing.s4),
            )
        }
        AnimatedVisibility(open, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut() + scaleOut(targetScale = 0.9f)) {
            PreviewPanel(open = open, onClose = { open = false })
        }
    }
}

@Composable
private fun PreviewPanel(
    open: Boolean,
    onClose: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    val scope = rememberCoroutineScope()
    // Composed only while open, so it registers after the page's own back handling and gets the press first.
    if (open) {
        PredictiveBackHandler { events ->
            try {
                events.collect { event ->
                    swipeEdge = event.swipeEdge
                    progress.snapTo(event.progress)
                }
                onClose()
            } catch (e: CancellationException) {
                scope.launch { progress.animateTo(0f, spring()) }
                throw e
            }
        }
    }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        modifier =
            Modifier.fillMaxWidth().graphicsLayer {
                // Read in the draw phase: the gesture updates only the layer, never recomposes.
                val p = progress.value
                val scale = 1f - (1f - PANEL_MIN_SCALE) * p
                scaleX = scale
                scaleY = scale
                val direction = if (swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                translationX = direction * PanelShift.toPx() * p
            },
    ) {
        Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            Text(stringResource(R.string.anim_transitions_preview_body), style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = onClose) {
                Text(stringResource(R.string.anim_transitions_close_preview))
            }
        }
    }
}
