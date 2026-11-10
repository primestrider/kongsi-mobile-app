package com.primestrider.kongsi.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icons that `material-icons-core` lacks, drawn from Material Symbols and Material Icons (Apache 2.0) so the app does not need the large
 * `material-icons-extended` artifact. Tint them like any `Icons.Filled` icon.
 */
object AppIcons {
    /** A list of items with a plus (Material Symbols Rounded `list_alt_add`, filled): "New bill". */
    val ListAdd: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "ListAdd",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f,
            ).addGroup(translationY = 960f) // Material Symbols use the viewBox "0 -960 960 960".
            .addPath(pathData = addPathNodes(LIST_ADD_PATH), fill = SolidColor(Color.Black))
            .clearGroup()
            .build()
    }

    /** A camera (Material Icons `photo_camera`, filled): "Take a photo" of a receipt. */
    val Camera: ImageVector by lazy { icon24("Camera", CAMERA_PATH) }

    /** A picture (Material Icons `image`, filled): "Choose from gallery". */
    val Photo: ImageVector by lazy { icon24("Photo", PHOTO_PATH) }
}

/** A filled icon on the classic Material Icons 24×24 grid. */
private fun icon24(
    name: String,
    path: String,
): ImageVector =
    ImageVector
        .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
        .build()

private const val CAMERA_PATH =
    "M12,12m-3.2,0a3.2,3.2 0,1 1,6.4 0a3.2,3.2 0,1 1,-6.4 0" +
        "M9,2L7.17,4L4,4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2L22,6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2L9,2z" +
        "M12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z"

private const val PHOTO_PATH =
    "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2z" +
        "M8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z"

private const val LIST_ADD_PATH =
    "M640-440q17 0 28.5-11.5T680-480q0-17-11.5-28.5T640-520H480q-17 0-28.5 11.5T440-480q0 17 11.5 28.5T48" +
        "0-440h160Zm0-160q17 0 28.5-11.5T680-640q0-17-11.5-28.5T640-680H480q-17 0-28.5 11.5T440-640q0 17 11.5" +
        " 28.5T480-600h160Zm40 440h-80q-17 0-28.5-11.5T560-200q0-17 11.5-28.5T600-240h80v-80q0-17 11.5-28.5T7" +
        "20-360q17 0 28.5 11.5T760-320v80h80q17 0 28.5 11.5T880-200q0 17-11.5 28.5T840-160h-80v80q0 17-11.5 2" +
        "8.5T720-40q-17 0-28.5-11.5T680-80v-80ZM320-280q17 0 28.5-11.5T360-320q0-17-11.5-28.5T320-360q-17 0-2" +
        "8.5 11.5T280-320q0 17 11.5 28.5T320-280Zm0-160q17 0 28.5-11.5T360-480q0-17-11.5-28.5T320-520q-17 0-2" +
        "8.5 11.5T280-480q0 17 11.5 28.5T320-440Zm0-160q17 0 28.5-11.5T360-640q0-17-11.5-28.5T320-680q-17 0-2" +
        "8.5 11.5T280-640q0 17 11.5 28.5T320-600ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840" +
        "h560q33 0 56.5 23.5T840-760v301q0 18-15.5 28t-32.5 3q-17-5-35.5-8t-36.5-3q-53 0-99 21t-79 58h-62q-17" +
        " 0-28.5 11.5T440-320q0 17 11.5 28.5T480-280h14q-7 19-10.5 39t-3.5 41q0 8 .5 17t1.5 17q2 18-8.5 32T44" +
        "6-120H200Z"
