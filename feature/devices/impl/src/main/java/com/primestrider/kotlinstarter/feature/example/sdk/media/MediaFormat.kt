package com.primestrider.kotlinstarter.feature.example.sdk.media

import java.util.Locale

/** `m:ss` for a duration in milliseconds (minutes are not capped); partial seconds are dropped, negatives read as 0. */
fun formatClock(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1000
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
