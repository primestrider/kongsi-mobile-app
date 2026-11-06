package com.primestrider.kotlinstarter.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.primestrider.kotlinstarter.core.designsystem.R

val PlusJakartaSans =
    FontFamily(
        Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
        Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
        Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
        Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
    )

object FontSize {
    val xs = 12.sp
    val sm = 14.sp
    val base = 16.sp
    val lg = 18.sp
    val xl = 20.sp
    val xl2 = 24.sp
    val xl3 = 30.sp
    val xl4 = 36.sp
    val xl5 = 48.sp
    val xl6 = 60.sp
}

object LineHeight {
    val xs = 16.sp
    val sm = 20.sp
    val base = 24.sp
    val lg = 28.sp
    val xl = 28.sp
    val xl2 = 32.sp
    val xl3 = 36.sp
    val xl4 = 40.sp
    val xl5 = 48.sp
    val xl6 = 60.sp
}

object LetterSpacing {
    val tighter = (-0.8).sp
    val tight = (-0.4).sp
    val normal = 0.sp
    val wide = 0.4.sp
    val wider = 0.8.sp
    val widest = 1.6.sp
}

private fun style(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight,
    letterSpacing: TextUnit = LetterSpacing.normal,
) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontSize = size,
    lineHeight = lineHeight,
    fontWeight = weight,
    letterSpacing = letterSpacing,
)

/** Material 3 typography mapped from the React Native AppText variants. */
val AppTypography =
    Typography(
        displayLarge = style(FontSize.xl6, LineHeight.xl6, FontWeight.Bold),
        displayMedium = style(FontSize.xl5, LineHeight.xl5, FontWeight.Bold),
        displaySmall = style(FontSize.xl4, LineHeight.xl4, FontWeight.Bold),
        headlineLarge = style(FontSize.xl3, LineHeight.xl3, FontWeight.ExtraBold, LetterSpacing.tight),
        headlineMedium = style(FontSize.xl2, LineHeight.xl2, FontWeight.Bold, LetterSpacing.tight),
        headlineSmall = style(FontSize.xl, LineHeight.xl, FontWeight.SemiBold),
        titleLarge = style(FontSize.lg, LineHeight.lg, FontWeight.SemiBold),
        titleMedium = style(FontSize.base, LineHeight.base, FontWeight.SemiBold),
        titleSmall = style(FontSize.sm, LineHeight.sm, FontWeight.SemiBold),
        bodyLarge = style(FontSize.base, LineHeight.base, FontWeight.Normal),
        bodyMedium = style(FontSize.sm, LineHeight.sm, FontWeight.Normal),
        bodySmall = style(FontSize.xs, LineHeight.xs, FontWeight.Normal),
        labelLarge = style(FontSize.sm, LineHeight.sm, FontWeight.Medium),
        labelMedium = style(FontSize.xs, LineHeight.xs, FontWeight.Medium),
        labelSmall = style(FontSize.xs, LineHeight.xs, FontWeight.Medium),
    )
