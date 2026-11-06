package com.primestrider.kotlinstarter.feature.example.sdk.location

import com.google.android.gms.location.Priority
import java.util.Locale
import kotlin.math.roundToInt

/** [value] with 6 decimals and a dot in every locale: coordinates are data, not prose. */
fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

/** Horizontal accuracy as "±13 m". */
fun formatAccuracy(meters: Float): String = "±${meters.roundToInt()} m"

/** High accuracy needs the fine permission; with only approximate access the request is made at balanced power instead. */
fun effectivePriority(
    requested: Int,
    fineGranted: Boolean,
): Int = if (requested == Priority.PRIORITY_HIGH_ACCURACY && !fineGranted) Priority.PRIORITY_BALANCED_POWER_ACCURACY else requested
