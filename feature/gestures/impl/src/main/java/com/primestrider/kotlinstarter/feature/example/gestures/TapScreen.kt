package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.rememberSaveableStateList
import com.primestrider.kotlinstarter.feature.gestures.impl.R
import kotlin.math.roundToInt

private const val FILE = "TapScreen.kt"

/** Tap gestures: `clickable`, `combinedClickable` and `detectTapGestures`. */
@Composable
fun TapScreen(onBack: () -> Unit) {
    ExamplePageScaffold(R.string.gesture_tap_title, R.string.gesture_tap_subtitle, onBack) {
        ClickableExample()
        CombinedClickableExample()
        DetectTapExample()
    }
}

@Composable
private fun ClickableExample() {
    var taps by rememberSaveable { mutableIntStateOf(0) }
    DemoSection(R.string.gesture_tap_clickable_title, R.string.gesture_tap_clickable_usage, "Modifier.clickable", FILE) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { taps++ }
                .testTag("clickTarget"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.gesture_tap_clickable_label), color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(stringResource(R.string.gesture_tap_count, taps), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CombinedClickableExample() {
    // String resource ids of the last five events, newest first, so the log follows the app language.
    val log = rememberSaveableStateList<Int>()

    fun record(event: Int) {
        log.add(0, event)
        while (log.size > 5) log.removeAt(log.lastIndex)
    }
    DemoSection(R.string.gesture_tap_combined_title, R.string.gesture_tap_combined_usage, "Modifier.combinedClickable", FILE) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .combinedClickable(
                    onLongClickLabel = stringResource(R.string.gesture_tap_long_label),
                    onLongClick = { record(R.string.gesture_tap_event_long) },
                    onDoubleClick = { record(R.string.gesture_tap_event_double) },
                    onClick = { record(R.string.gesture_tap_event_click) },
                ).testTag("combinedTarget"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.gesture_tap_combined_hint), color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        if (log.isEmpty()) {
            Text(stringResource(R.string.gesture_tap_log_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            Column {
                log.forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun DetectTapExample() {
    // NaN = nothing touched yet; plain floats keep the mark across rotation without a custom saver.
    var x by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var y by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var kind by rememberSaveable { mutableIntStateOf(R.string.gesture_tap_kind_tap) }
    var pressed by remember { mutableStateOf(false) }

    fun mark(
        at: Offset,
        what: Int,
    ) {
        x = at.x
        y = at.y
        kind = what
    }
    DemoSection(R.string.gesture_tap_detect_title, R.string.gesture_tap_detect_usage, "pointerInput { detectTapGestures() }", FILE) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (pressed) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                ).pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onTap = { mark(it, R.string.gesture_tap_kind_tap) },
                        onDoubleTap = { mark(it, R.string.gesture_tap_kind_double) },
                        onLongPress = { mark(it, R.string.gesture_tap_kind_long) },
                    )
                }.testTag("tapArea"),
        ) {
            Text(
                stringResource(if (pressed) R.string.gesture_tap_pressed else R.string.gesture_tap_detect_hint),
                modifier = Modifier.align(Alignment.Center).padding(Spacing.s2),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!x.isNaN()) {
                Box(
                    Modifier
                        .offset { IntOffset(x.roundToInt() - 12.dp.roundToPx(), y.roundToInt() - 12.dp.roundToPx()) }
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("tapMark"),
                )
            }
        }
        if (!x.isNaN()) {
            Text(
                stringResource(R.string.gesture_tap_last, stringResource(kind), x.roundToInt(), y.roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun TapScreenPreview() {
    PreviewTheme {
        TapScreen(onBack = {})
    }
}
