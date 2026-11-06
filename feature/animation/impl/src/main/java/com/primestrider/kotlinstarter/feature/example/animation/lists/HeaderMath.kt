package com.primestrider.kotlinstarter.feature.example.animation.lists

/** How far the header has collapsed, 0 (fully open) to 1 (fully collapsed); no range to collapse counts as collapsed. */
fun collapseFraction(
    scrollPx: Int,
    rangePx: Int,
): Float = if (rangePx <= 0) 1f else (scrollPx.toFloat() / rangePx).coerceIn(0f, 1f)

/** Downward shift of the header image for a scroll of [scrollPx], so it trails the content; never negative. */
fun parallaxOffset(
    scrollPx: Int,
    factor: Float = 0.5f,
): Float = (scrollPx * factor).coerceAtLeast(0f)
