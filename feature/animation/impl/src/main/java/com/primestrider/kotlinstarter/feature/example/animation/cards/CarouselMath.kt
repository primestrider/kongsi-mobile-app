package com.primestrider.kotlinstarter.feature.example.animation.cards

import kotlin.math.abs

private fun lerpByDistance(
    pageOffset: Float,
    minValue: Float,
): Float = 1f - (1f - minValue) * abs(pageOffset).coerceIn(0f, 1f)

/** Scale of a carousel page: 1 when centred, falling linearly to [minScale] one page (or more) away. */
fun carouselScale(
    pageOffset: Float,
    minScale: Float = 0.85f,
): Float = lerpByDistance(pageOffset, minScale)

/** Opacity of a carousel page: 1 when centred, falling linearly to [minAlpha] one page (or more) away. */
fun carouselAlpha(
    pageOffset: Float,
    minAlpha: Float = 0.6f,
): Float = lerpByDistance(pageOffset, minAlpha)
