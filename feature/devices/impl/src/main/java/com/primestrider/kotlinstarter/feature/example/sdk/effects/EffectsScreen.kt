package com.primestrider.kotlinstarter.feature.example.sdk.effects

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.component.LabeledSlider
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.core.util.formatCurrency
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import kotlin.math.roundToInt

private const val BALANCE = 1_250_000.0

private enum class Palette(
    @StringRes val label: Int,
    val from: Color,
    val to: Color,
) {
    OCEAN(R.string.sdk_effects_ocean, Color(0xFF0EA5E9), Color(0xFF6366F1)),
    SUNSET(R.string.sdk_effects_sunset, Color(0xFFF97316), Color(0xFFEC4899)),
    FOREST(R.string.sdk_effects_forest, Color(0xFF22C55E), Color(0xFF0F766E)),
}

/** Brush gradients at any angle, a blurred balance, and a 3×3 AGSL mesh (API 33+). */
@Composable
fun EffectsScreen(onBack: () -> Unit) {
    val locale = currentLocale()
    val balance = formatCurrency(BALANCE, "IDR", locale, fractionDigits = 0)
    val paletteLabels = Palette.entries.associateWith { stringResource(it.label) }
    var palette by rememberSaveable { mutableStateOf(Palette.OCEAN) }
    var angle by rememberSaveable { mutableFloatStateOf(45f) }
    var hidden by rememberSaveable { mutableStateOf(true) }
    var intensity by rememberSaveable { mutableFloatStateOf(60f) }
    var middle by rememberSaveable { mutableFloatStateOf(0.5f) }
    var smooth by rememberSaveable { mutableStateOf(true) }

    ExamplePageScaffold(title = R.string.sdk_effects_title, subtitle = R.string.sdk_effects_subtitle, onBack = onBack) {
        SdkSection(R.string.sdk_effects_linear_title, R.string.sdk_effects_linear_description) {
            WalletCard(
                balance,
                Modifier.drawBehind {
                    val (start, end) = gradientOffsets(angle, size)
                    drawRect(Brush.linearGradient(listOf(palette.from, palette.to), start, end))
                },
            )
            SegmentedChoice(
                options = Palette.entries,
                selected = palette,
                onSelect = { palette = it },
                label = { paletteLabels.getValue(it) },
            )
            LabeledSlider(
                label = stringResource(R.string.sdk_effects_angle),
                value = angle,
                onValueChange = { angle = it },
                valueRange = 0f..180f,
                steps = 11,
                valueText = stringResource(R.string.sdk_effects_degrees, angle.roundToInt()),
            )
        }
        SdkSection(R.string.sdk_effects_blur_title, R.string.sdk_effects_blur_description) {
            BlurCard(balance, hidden, intensity)
            SwitchRow(stringResource(R.string.sdk_effects_hide_balance), hidden, { hidden = it })
            LabeledSlider(
                label = stringResource(R.string.sdk_effects_intensity),
                value = intensity,
                onValueChange = { intensity = it },
                valueRange = 0f..100f,
                steps = 19,
                valueText = intensity.roundToInt().toString(),
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) Caption(R.string.sdk_effects_blur_fallback)
        }
        SdkSection(R.string.sdk_effects_mesh_title, R.string.sdk_effects_mesh_description) {
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(Radii.lg))) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    MeshGradient(middle, smooth)
                } else {
                    Box(Modifier.fillMaxSize().background(fallbackBrush()))
                }
            }
            LabeledSlider(
                label = stringResource(R.string.sdk_effects_middle_point),
                value = middle,
                onValueChange = { middle = it },
                valueRange = 0.2f..0.8f,
                valueText = "%.2f".format(locale, middle),
            )
            SwitchRow(stringResource(R.string.sdk_effects_smooth), smooth, { smooth = it })
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Caption(R.string.sdk_effects_mesh_fallback)
        }
    }
}

@Composable
private fun WalletCard(
    balance: String,
    background: Modifier,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(Radii.lg))
            .then(background)
            .padding(Spacing.s4),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.sdk_effects_wallet_caption), style = MaterialTheme.typography.labelLarge, color = Color.White)
        Text(balance, style = MaterialTheme.typography.headlineMedium, color = Color.White)
    }
}

/** The balance, frosted with a real blur from API 31 and with a translucent scrim below. */
@Composable
private fun BlurCard(
    balance: String,
    hidden: Boolean,
    intensity: Float,
) {
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(Radii.lg))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val blur = if (hidden && canBlur) Modifier.blur(blurRadius(intensity), BlurredEdgeTreatment.Rectangle) else Modifier
        Text(balance, style = MaterialTheme.typography.headlineMedium, modifier = blur)
        if (hidden) {
            if (!canBlur) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)))
            Text(
                stringResource(R.string.sdk_effects_balance_hidden),
                style = MaterialTheme.typography.titleMedium,
                color = if (canBlur) MaterialTheme.colorScheme.onSurface else Color.White,
            )
        }
    }
}

@Composable
private fun Caption(
    @StringRes text: Int,
) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@ThemePreviews
@Composable
private fun EffectsScreenPreview() {
    PreviewTheme {
        EffectsScreen(onBack = {})
    }
}
