package com.primestrider.kotlinstarter.feature.example.sdk.effects

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb

/** Row-major 3×3 control colors of the mesh. */
val MeshColors: List<Color> =
    listOf(
        Color(0xFF6366F1),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFF0EA5E9),
        Color(0xFFF8FAFC),
        Color(0xFFF97316),
        Color(0xFF22C55E),
        Color(0xFF14B8A6),
        Color(0xFFEAB308),
    )

// The two axes are remapped so the middle control line sits at `mid`, then the 3×3 colors are interpolated
// bilinearly (smoothstep-eased between control points when `smoothColors` is set).
private const val MESH_AGSL = """
uniform float2 size;
uniform float2 mid;
uniform float smoothColors;
uniform float4 colors[9];

float remap(float t, float m) {
    return t < m ? 0.5 * t / m : 0.5 + 0.5 * (t - m) / (1.0 - m);
}

float4 colorAt(int x, int y) {
    int index = y * 3 + x;
    for (int i = 0; i < 9; i++) {
        if (i == index) return colors[i];
    }
    return colors[0];
}

half4 main(float2 coord) {
    float2 uv = clamp(coord / size, 0.0, 1.0);
    float2 g = float2(remap(uv.x, mid.x), remap(uv.y, mid.y)) * 2.0;
    int x = int(min(floor(g.x), 1.0));
    int y = int(min(floor(g.y), 1.0));
    float2 f = g - float2(float(x), float(y));
    if (smoothColors > 0.5) f = f * f * (3.0 - 2.0 * f);
    float4 top = mix(colorAt(x, y), colorAt(x + 1, y), f.x);
    float4 bottom = mix(colorAt(x, y + 1), colorAt(x + 1, y + 1), f.x);
    return half4(mix(top, bottom, f.y));
}
"""

/**
 * The 3×3 mesh gradient as an AGSL shader (API 33+). [middle] (0.2..0.8) moves the center control point on
 * both axes. Below API 33 the caller shows [fallbackBrush] instead.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MeshGradient(
    middle: Float,
    smoothColors: Boolean,
    modifier: Modifier = Modifier,
    colors: List<Color> = MeshColors,
) {
    val shader = remember { RuntimeShader(MESH_AGSL) }
    val floats = remember(colors) { FloatArray(colors.size * 4).also { colors.fillFloats(it) } }
    Canvas(modifier.fillMaxSize()) {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("mid", middle, middle)
        shader.setFloatUniform("smoothColors", if (smoothColors) 1f else 0f)
        shader.setFloatUniform("colors", floats)
        drawRect(ShaderBrush(shader))
    }
}

/** Below API 33: the control colors as a plain diagonal linear gradient. */
fun fallbackBrush(colors: List<Color> = MeshColors): Brush = Brush.linearGradient(colors)

private fun List<Color>.fillFloats(out: FloatArray) {
    forEachIndexed { i, color ->
        val argb = color.toArgb()
        out[i * 4] = ((argb shr 16) and 0xFF) / 255f
        out[i * 4 + 1] = ((argb shr 8) and 0xFF) / 255f
        out[i * 4 + 2] = (argb and 0xFF) / 255f
        out[i * 4 + 3] = ((argb shr 24) and 0xFF) / 255f
    }
}
