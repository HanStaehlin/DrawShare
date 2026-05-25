package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
)

@Serializable
data class DrawStroke(
    val points: List<StrokePoint>,
    val colorArgb: Int,
    val width: Float,
    val alpha: Float,
    val isEraser: Boolean = false,
)
