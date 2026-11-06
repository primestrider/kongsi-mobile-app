package com.primestrider.kotlinstarter.core.designsystem.theme

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
            secondaryFixed = Palette.Gray.c100,
            secondaryFixedDim = Palette.Gray.c200,
            onSecondaryFixed = Palette.Gray.c900,
            onSecondaryFixedVariant = Palette.Gray.c700,
            tertiaryFixed = Palette.Blue.c100,
            tertiaryFixedDim = Palette.Blue.c200,
            onTertiaryFixed = Palette.Blue.c900,
            onTertiaryFixedVariant = Palette.Blue.c700,
        )
    return if (dark) {
        base.copy(
            primaryContainer = Palette.Primary.c800,
            onPrimaryContainer = Palette.Primary.c100,
            inversePrimary = Palette.Primary.c600,
            secondary = Palette.Gray.c300,
            onSecondary = Palette.Gray.c950,
            tertiaryContainer = Palette.Blue.c900,
            onTertiaryContainer = Palette.Blue.c100,
            errorContainer = Palette.Red.c950,
            onErrorContainer = Palette.Red.c200,
            surfaceDim = Palette.Gray.c950,
            surfaceBright = Palette.Gray.c800,
            surfaceContainerLowest = Palette.Gray.c950,
            surfaceContainerLow = Palette.Gray.c900,
            surfaceContainer = Palette.Gray.c900,
            surfaceContainerHigh = Palette.Gray.c800,
            surfaceContainerHighest = Palette.Gray.c700,
            inverseSurface = Palette.Gray.c100,
            inverseOnSurface = Palette.Gray.c900,
        )
    } else {
        base.copy(
            primaryContainer = Palette.Primary.c100,
            onPrimaryContainer = Palette.Primary.c900,
            inversePrimary = Palette.Primary.c300,
            secondary = Palette.Gray.c700,
            onSecondary = Palette.white,
            tertiaryContainer = Palette.Info.c50,
            onTertiaryContainer = Palette.Info.c700,
            errorContainer = Palette.Error.c50,
            onErrorContainer = Palette.Error.c700,
            surfaceDim = Palette.Gray.c100,
            surfaceBright = Palette.white,
            surfaceContainerLowest = Palette.white,
            surfaceContainerLow = Palette.Gray.c50,
            surfaceContainer = Palette.Gray.c100,
            surfaceContainerHigh = Palette.Gray.c200,
            surfaceContainerHighest = Palette.Gray.c300,
            inverseSurface = Palette.Gray.c800,
            inverseOnSurface = Palette.Gray.c50,
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
