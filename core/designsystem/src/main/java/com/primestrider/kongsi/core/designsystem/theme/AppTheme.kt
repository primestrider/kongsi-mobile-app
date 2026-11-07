package com.primestrider.kongsi.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val AppShapes =
    Shapes(
        small = RoundedCornerShape(Radii.sm),
        medium = RoundedCornerShape(Radii.md),
        large = RoundedCornerShape(Radii.lg),
        extraLarge = RoundedCornerShape(Radii.xl),
    )

/**
 * Maps every Material 3 role to a token, so stock M3 components (FAB, chips, snackbar, …)
 * never fall back to the M3 baseline purple. See docs/architecture/design-system.md.
 */
private fun AppColors.toColorScheme(dark: Boolean): ColorScheme {
    val base =
        (if (dark) darkColorScheme() else lightColorScheme()).copy(
            primary = primary,
            onPrimary = primaryForeground,
            background = background,
            onBackground = foreground,
            surface = card,
            onSurface = cardForeground,
            surfaceVariant = secondary,
            onSurfaceVariant = muted,
            secondaryContainer = secondary,
            onSecondaryContainer = secondaryForeground,
            error = destructive,
            onError = destructiveForeground,
            outline = border,
            outlineVariant = border,
            tertiary = info,
            onTertiary = Palette.white,
            surfaceTint = primary,
            scrim = Palette.black,
            // "Fixed" roles keep the same value in light and dark by Material 3 design.
            primaryFixed = Palette.Primary.c100,
            primaryFixedDim = Palette.Primary.c200,
            onPrimaryFixed = Palette.Primary.c900,
            onPrimaryFixedVariant = Palette.Primary.c700,
            secondaryFixed = Palette.Stone.c100,
            secondaryFixedDim = Palette.Stone.c200,
            onSecondaryFixed = Palette.Stone.c900,
            onSecondaryFixedVariant = Palette.Stone.c700,
            tertiaryFixed = Palette.Blue.c100,
            tertiaryFixedDim = Palette.Blue.c200,
            onTertiaryFixed = Palette.Blue.c900,
            onTertiaryFixedVariant = Palette.Blue.c700,
        )
    return if (dark) {
        base.copy(
            // Dark butter fills turn a murky mustard; on dark, highlights are ink with butter text instead.
            primaryContainer = Palette.Ink.c800,
            onPrimaryContainer = Palette.Primary.c200,
            // Snackbars: a pale ink slip with a golden-brown action.
            inversePrimary = Palette.Primary.c800,
            secondary = Palette.Stone.c300,
            onSecondary = Palette.Stone.c950,
            tertiaryContainer = Palette.Blue.c900,
            onTertiaryContainer = Palette.Blue.c100,
            errorContainer = Palette.Red.c950,
            onErrorContainer = Palette.Red.c200,
            surfaceDim = Palette.Stone.c950,
            surfaceBright = Palette.Stone.c800,
            surfaceContainerLowest = Palette.Stone.c950,
            surfaceContainerLow = Palette.Stone.c900,
            surfaceContainer = Palette.Stone.c900,
            surfaceContainerHigh = Palette.Stone.c800,
            surfaceContainerHighest = Palette.Stone.c700,
            inverseSurface = Palette.Ink.c100,
            inverseOnSurface = Palette.Ink.c950,
        )
    } else {
        base.copy(
            primaryContainer = Palette.Primary.c100,
            onPrimaryContainer = Palette.Primary.c900,
            // Snackbars: an ink slip with a butter action.
            inversePrimary = Palette.Primary.c300,
            secondary = Palette.Stone.c700,
            onSecondary = Palette.white,
            tertiaryContainer = Palette.Info.c50,
            onTertiaryContainer = Palette.Info.c700,
            errorContainer = Palette.Error.c50,
            onErrorContainer = Palette.Error.c700,
            surfaceDim = Palette.Stone.c100,
            surfaceBright = Palette.white,
            surfaceContainerLowest = Palette.white,
            surfaceContainerLow = Palette.Stone.c50,
            surfaceContainer = Palette.Stone.c100,
            surfaceContainerHigh = Palette.Stone.c200,
            surfaceContainerHighest = Palette.Stone.c300,
            inverseSurface = Palette.Ink.c800,
            inverseOnSurface = Palette.Stone.c50,
        )
    }
}

@Composable
fun AppTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object AppThemeTokens {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}
