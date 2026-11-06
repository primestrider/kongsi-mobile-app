package com.primestrider.kotlinstarter.feature.example.animation.transitions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.animation.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.animation.AnimExample

/** Height of the demo frame: tall enough for the three cards and the detail page. */
private val DemoFrameHeight = 340.dp

/**
 * Page transitions: a nested NavHost demo that plays the chosen Material pattern, one card per pattern saying when
 * to use it, and a predictive back example.
 *
 * Back handling: while a demo detail is open the nested NavHost's own handler pops it (and follows the predictive
 * back gesture); on the demo list that handler is disabled, so back falls through to the app's NavHost, which
 * leaves the page with its own predictive back. [onBack] is only the top bar's arrow.
 */
@Composable
fun TransitionsScreen(onBack: () -> Unit) {
    val pattern = rememberSaveable { mutableStateOf(TransitionPattern.CONTAINER_TRANSFORM) }
    val demoNav = rememberNavController()
    ExamplePageScaffold(R.string.anim_transitions_title, R.string.anim_transitions_subtitle, onBack) {
        AnimExample(
            R.string.anim_transitions_demo_title,
            R.string.anim_transitions_demo_usage,
            "NavHost + composable(enterTransition, popExitTransition)",
        ) {
            PatternPicker(pattern)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(DemoFrameHeight)
                    .testTag("demoFrame")
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                DemoNavHost(demoNav, pattern, Modifier.fillMaxSize())
            }
        }
        TransitionPattern.entries.forEach { AnimExample(it.label, it.usage, it.api) {} }
        PredictiveBackDemo()
    }
}

@Composable
private fun PatternPicker(pattern: MutableState<TransitionPattern>) {
    FlowRow(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        TransitionPattern.entries.forEach { option ->
            // Selection shows as the filled container versus the outline (no icon, so chip widths never change and
            // the row does not rewrap); one-of-N, so it reports a radio button rather than the chip's checkbox.
            FilterChip(
                selected = option == pattern.value,
                onClick = { pattern.value = option },
                label = { Text(stringResource(option.label)) },
                modifier = Modifier.semantics { role = Role.RadioButton },
            )
        }
    }
}

@ThemePreviews
@Composable
private fun TransitionsScreenPreview() {
    PreviewTheme {
        TransitionsScreen(onBack = {})
    }
}
