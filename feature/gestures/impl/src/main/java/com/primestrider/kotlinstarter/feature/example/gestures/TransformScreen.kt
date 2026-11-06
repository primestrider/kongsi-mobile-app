package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.gestures.impl.R
import kotlin.math.roundToInt

private const val FILE = "TransformScreen.kt"

/** Zoom step of the Zoom in / Zoom out buttons. */
private const val ZOOM_STEP = 0.5f

/** Multi-touch transforms: pinch to zoom, pan and rotate, double tap to zoom, `transformable`. */
@Composable
fun TransformScreen(onBack: () -> Unit) {
    ExamplePageScaffold(R.string.gesture_transform_title, R.string.gesture_transform_subtitle, onBack) {
        PinchZoomExample()
        RotateExample()
    }
}

@Composable
private fun PinchZoomExample() {
    var scale by rememberSaveable { mutableFloatStateOf(MIN_ZOOM) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    var rotation by rememberSaveable { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(Size.Zero) }

    fun apply(
        newScale: Float,
        pan: Offset = Offset(panX, panY),
    ) {
        scale = newScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val clamped = clampPan(pan, scale, size)
        panX = clamped.x
        panY = clamped.y
    }
    DemoSection(
        R.string.gesture_transform_pinch_title,
        R.string.gesture_transform_pinch_usage,
        "detectTransformGestures + detectTapGestures(onDoubleTap)",
        FILE,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { size = it.toSize() }
                // Every pointer change is consumed here, so a drag on the photo never scrolls the page.
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotate ->
                        rotation += rotate
                        apply(scale * zoom, Offset(panX, panY) + pan)
                    }
                }.pointerInput(Unit) { detectTapGestures(onDoubleTap = { apply(toggleZoom(scale)) }) }
                .testTag("transformSurface"),
        ) {
            PhotoStandIn(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = panX
                        translationY = panY
                        rotationZ = rotation
                    },
            )
        }
        Text(stringResource(R.string.gesture_transform_zoom, scale), style = MaterialTheme.typography.bodyMedium)
        val zoomIn = stringResource(R.string.gesture_transform_zoom_in)
        val zoomOut = stringResource(R.string.gesture_transform_zoom_out)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { apply(scale - ZOOM_STEP) }, modifier = Modifier.semantics { contentDescription = zoomOut }) {
                Text("−")
            }
            FilledTonalIconButton(onClick = { apply(scale + ZOOM_STEP) }, modifier = Modifier.semantics { contentDescription = zoomIn }) {
                Text("+")
            }
            TextButton(onClick = {
                rotation = 0f
                apply(MIN_ZOOM, Offset.Zero)
            }) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(stringResource(R.string.gesture_transform_reset))
            }
        }
    }
}

@Composable
private fun RotateExample() {
    var angle by rememberSaveable { mutableFloatStateOf(0f) }
    val state = rememberTransformableState { _, _, rotate -> angle += rotate }
    DemoSection(
        R.string.gesture_transform_rotate_title,
        R.string.gesture_transform_rotate_usage,
        "Modifier.transformable(rememberTransformableState())",
        FILE,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s4), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .transformable(state)
                    .testTag("rotateSurface"),
                contentAlignment = Alignment.Center,
            ) {
                PhotoStandIn(Modifier.size(96.dp).graphicsLayer { rotationZ = angle })
            }
            TextButton(onClick = { angle += 15f }) { Text(stringResource(R.string.gesture_transform_rotate_step)) }
        }
        Text(stringResource(R.string.gesture_transform_angle, angle.roundToInt()), style = MaterialTheme.typography.bodyMedium)
    }
}

/** A gradient with a star: stands in for a photo, so the page needs no image assets or network. */
@Composable
private fun PhotoStandIn(modifier: Modifier) {
    Box(
        modifier.background(
            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
    }
}

@ThemePreviews
@Composable
private fun TransformScreenPreview() {
    PreviewTheme {
        TransformScreen(onBack = {})
    }
}
