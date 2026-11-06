package com.primestrider.kotlinstarter.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic color roles, mirroring the React Native `colors` / `darkColors` tokens. */
@Immutable
data class AppColors(
    val background: Color,
    val foreground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val border: Color,
    val card: Color,
    val cardForeground: Color,
    val primary: Color,
    val primaryForeground: Color,
    val secondary: Color,
    val secondaryForeground: Color,
    val destructive: Color,
    val destructiveForeground: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
)

val LightColors =
    AppColors(
        background = Palette.white,
        foreground = Palette.Gray.c900,
        muted = Palette.Gray.c500,
        mutedForeground = Palette.Gray.c400,
        border = Palette.Gray.c200,
        card = Palette.white,
        cardForeground = Palette.Gray.c900,
        primary = Palette.Primary.c500,
        primaryForeground = Palette.white,
        secondary = Palette.Gray.c100,
        secondaryForeground = Palette.Gray.c900,
        destructive = Palette.Error.c500,
        destructiveForeground = Palette.white,
        success = Palette.Success.c500,
        warning = Palette.Warning.c500,
        info = Palette.Info.c500,
    )

val DarkColors =
    AppColors(
        background = Palette.Gray.c950,
        foreground = Palette.Gray.c50,
        muted = Palette.Gray.c400,
        mutedForeground = Palette.Gray.c500,
        border = Palette.Gray.c800,
        card = Palette.Gray.c900,
        cardForeground = Palette.Gray.c50,
        primary = Palette.Primary.c400,
        primaryForeground = Palette.Gray.c950,
        secondary = Palette.Gray.c800,
        secondaryForeground = Palette.Gray.c50,
        destructive = Palette.Error.c500,
        destructiveForeground = Palette.white,
        success = Palette.Success.c500,
        warning = Palette.Warning.c500,
        info = Palette.Info.c500,
    )

val LocalAppColors = staticCompositionLocalOf { LightColors }
