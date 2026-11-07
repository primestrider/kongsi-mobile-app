package com.primestrider.kongsi.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic color roles, "ink and paper": ink actions on paper (light) or pale ink actions on a dark page (dark), with butter
 * ([Palette.Primary]) as the only accent, through the Material container roles. */
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
        background = Palette.Stone.c50,
        foreground = Palette.Stone.c900,
        muted = Palette.Stone.c500,
        mutedForeground = Palette.Stone.c400,
        border = Palette.Stone.c200,
        card = Palette.white,
        cardForeground = Palette.Stone.c900,
        primary = Palette.Ink.c800,
        primaryForeground = Palette.Stone.c50,
        // One step darker than usual: the background is already off-white, and fields and radios need to show on it.
        secondary = Palette.Stone.c200,
        secondaryForeground = Palette.Stone.c900,
        destructive = Palette.Error.c500,
        destructiveForeground = Palette.white,
        success = Palette.Success.c500,
        warning = Palette.Warning.c500,
        info = Palette.Info.c500,
    )

val DarkColors =
    AppColors(
        background = Palette.Stone.c950,
        foreground = Palette.Stone.c50,
        muted = Palette.Stone.c400,
        mutedForeground = Palette.Stone.c500,
        border = Palette.Stone.c800,
        card = Palette.Stone.c900,
        cardForeground = Palette.Stone.c50,
        primary = Palette.Ink.c100,
        primaryForeground = Palette.Ink.c950,
        secondary = Palette.Stone.c800,
        secondaryForeground = Palette.Stone.c50,
        destructive = Palette.Error.c500,
        destructiveForeground = Palette.white,
        success = Palette.Success.c500,
        warning = Palette.Warning.c500,
        info = Palette.Info.c500,
    )

val LocalAppColors = staticCompositionLocalOf { LightColors }
