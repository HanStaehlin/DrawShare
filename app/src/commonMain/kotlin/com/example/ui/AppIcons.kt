package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Tilted eraser body with a diagonal band cut out. Band corners touch the
// outer edges so the band reads as a notch separating rubber from ferrule.
val EraserIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Eraser",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd,
        ) {
            // Outer parallelogram (eraser body, 45° tilt)
            moveTo(15f, 3f)
            lineTo(21f, 9f)
            lineTo(9f, 21f)
            lineTo(3f, 15f)
            close()
            // Band notch (~30% from top), perpendicular to long axis
            moveTo(11.5f, 6.5f)
            lineTo(17.5f, 12.5f)
            lineTo(16f, 14f)
            lineTo(10f, 8f)
            close()
        }
    }.build()
}
