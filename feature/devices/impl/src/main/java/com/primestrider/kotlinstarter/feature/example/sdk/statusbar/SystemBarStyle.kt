package com.primestrider.kotlinstarter.feature.example.sdk.statusbar

/** How the status bar icons are drawn on the System Bars demo. */
enum class BarStyle { AUTO, LIGHT_ICONS, DARK_ICONS, INVERTED }

/**
 * The value for `isAppearanceLightStatusBars` that gives [style]: true draws dark icons (for light bars),
 * false draws light icons (for dark bars). So LIGHT_ICONS is always false.
 */
fun lightIcons(
    style: BarStyle,
    darkTheme: Boolean,
): Boolean =
    when (style) {
        BarStyle.AUTO -> !darkTheme
        BarStyle.LIGHT_ICONS -> false
        BarStyle.DARK_ICONS -> true
        BarStyle.INVERTED -> darkTheme
    }
