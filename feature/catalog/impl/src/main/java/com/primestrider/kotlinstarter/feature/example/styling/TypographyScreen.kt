package com.primestrider.kotlinstarter.feature.example.styling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.primestrider.kotlinstarter.core.designsystem.theme.FontSize
import com.primestrider.kotlinstarter.core.designsystem.theme.LetterSpacing
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold

private data class StyleRow(
    val name: String,
    val rnVariant: String?,
    val style: TextStyle,
)

@Composable
fun TypographyScreen(onBack: () -> Unit) {
    val t = MaterialTheme.typography
    val styles =
        listOf(
            StyleRow("displayLarge", null, t.displayLarge),
            StyleRow("displayMedium", null, t.displayMedium),
            StyleRow("displaySmall", null, t.displaySmall),
            StyleRow("headlineLarge", "h1", t.headlineLarge),
            StyleRow("headlineMedium", "h2", t.headlineMedium),
            StyleRow("headlineSmall", "h3", t.headlineSmall),
            StyleRow("titleLarge", "title", t.titleLarge),
            StyleRow("titleMedium", "title", t.titleMedium),
            StyleRow("titleSmall", "title", t.titleSmall),
            StyleRow("bodyLarge", "body", t.bodyLarge),
            StyleRow("bodyMedium", "body", t.bodyMedium),
            StyleRow("bodySmall", "caption", t.bodySmall),
            StyleRow("labelLarge", "label", t.labelLarge),
            StyleRow("labelMedium", null, t.labelMedium),
            StyleRow("labelSmall", null, t.labelSmall),
        )
    val sizes =
        listOf(
            "xs" to FontSize.xs,
            "sm" to FontSize.sm,
            "base" to FontSize.base,
            "lg" to FontSize.lg,
            "xl" to FontSize.xl,
            "2xl" to FontSize.xl2,
            "3xl" to FontSize.xl3,
            "4xl" to FontSize.xl4,
            "5xl" to FontSize.xl5,
            "6xl" to FontSize.xl6,
        )
    val weights =
        listOf(
            400 to FontWeight.Normal,
            500 to FontWeight.Medium,
            600 to FontWeight.SemiBold,
            700 to FontWeight.Bold,
            800 to FontWeight.ExtraBold,
        )
    val spacings =
        listOf(
            "tighter" to LetterSpacing.tighter,
            "tight" to LetterSpacing.tight,
            "normal" to LetterSpacing.normal,
            "wide" to LetterSpacing.wide,
            "wider" to LetterSpacing.wider,
            "widest" to LetterSpacing.widest,
        )
    ExamplePageScaffold(
        title = R.string.example_styling_typography_title,
        subtitle = R.string.example_styling_typography_subtitle,
        onBack = onBack,
    ) {
        DemoSection(R.string.styling_section_text_styles) {
            styles.forEach { row ->
                val variant = row.rnVariant?.let { " · $it" }.orEmpty()
                SampleLine(row.name + variant, row.style)
            }
        }
        DemoSection(R.string.styling_section_font_sizes) {
            sizes.forEach { (name, size) -> SampleLine("$name · ${size.value.toInt()}sp", TextStyle(fontSize = size)) }
        }
        DemoSection(R.string.styling_section_font_weights) {
            weights.forEach { (value, weight) -> SampleLine("$value", TextStyle(fontWeight = weight)) }
        }
        DemoSection(R.string.styling_section_text_alignment) { TextAlignmentSamples() }
        DemoSection(R.string.styling_section_text_decoration) { TextDecorationSamples() }
        DemoSection(R.string.styling_section_letter_spacing) {
            spacings.forEach { (name, spacing) ->
                SampleLine("$name · ${spacing.value}sp", TextStyle(letterSpacing = spacing))
            }
        }
    }
}

@Composable
private fun SampleLine(
    label: String,
    style: TextStyle,
    text: String = stringResource(R.string.styling_sample_text),
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.merge(style),
        )
    }
}

@Composable
private fun TextAlignmentSamples() {
    listOf(
        "start" to TextAlign.Start,
        "center" to TextAlign.Center,
        "end" to TextAlign.End,
        "justify" to TextAlign.Justify,
    ).forEach { (name, align) ->
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.styling_sample_text).repeat(2),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = align,
            )
        }
    }
}

@Composable
private fun TextDecorationSamples() {
    SampleLine("underline", TextStyle(textDecoration = TextDecoration.Underline))
    SampleLine("lineThrough", TextStyle(textDecoration = TextDecoration.LineThrough))
    val sample = stringResource(R.string.styling_sample_text)
    // Compose has no text-transform style; transform the string itself.
    SampleLine("uppercase", TextStyle(), sample.uppercase())
    SampleLine("lowercase", TextStyle(), sample.lowercase())
}

@ThemePreviews
@Composable
private fun TypographyScreenPreview() {
    PreviewTheme {
        TypographyScreen(onBack = {})
    }
}
