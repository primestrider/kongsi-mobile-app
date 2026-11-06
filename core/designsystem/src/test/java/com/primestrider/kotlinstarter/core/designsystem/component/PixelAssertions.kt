package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertTrue
import kotlin.math.abs

/** Colour of the pixel [x], [y] (in dp from the node's top-left) of a captured node. Needs native graphics. */
fun SemanticsNodeInteraction.pixelAt(
    x: Dp,
    y: Dp,
    density: Density,
): Color {
    val image = captureToImage()
    val px = with(density) { x.roundToPx() }.coerceIn(0, image.width - 1)
    val py = with(density) { y.roundToPx() }.coerceIn(0, image.height - 1)
    return image.toPixelMap()[px, py]
}

/** Equal within [tolerance] per 0..255 channel, so blending rounding does not fail a check. */
fun assertColorClose(
    expected: Color,
    actual: Color,
    tolerance: Int = 3,
) {
    fun Float.c() = (this * 255).toInt()
    val close =
        abs(expected.red.c() - actual.red.c()) <= tolerance &&
            abs(expected.green.c() - actual.green.c()) <= tolerance &&
            abs(expected.blue.c() - actual.blue.c()) <= tolerance
    assertTrue("expected $expected but was $actual", close)
}
