package com.primestrider.kotlinstarter.feature.example.animation.cards

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** An archive box with a down arrow. material-icons-core has no archive icon, and the extended set is a large dependency. */
internal val ArchiveIcon: ImageVector by lazy {
    ImageVector
        .Builder("Archive", 24.dp, 24.dp, 24f, 24f)
        .addPath(
            PathParser()
                .parsePathString(
                    "M20.54,5.23l-1.39,-1.68C18.88,3.21 18.47,3 18,3L6,3c-0.47,0 -0.88,0.21 -1.15,0.55L3.46,5.23" +
                        "C3.17,5.57 3,6.02 3,6.5L3,19c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2L21,6.5c0,-0.48 -0.17,-0.93 -0.46,-1.27z" +
                        "M12,17.5L6.5,12L10,12v-2h4v2h3.5L12,17.5z" +
                        "M5.12,5l0.81,-1h12l0.94,1L5.12,5z",
                ).toNodes(),
            fill = SolidColor(Color.Black),
        ).build()
}
