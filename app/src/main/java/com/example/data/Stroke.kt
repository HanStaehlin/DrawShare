package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StrokePoint(
    val x: Float,
    val y: Float,
)

@JsonClass(generateAdapter = true)
data class DrawStroke(
    val points: List<StrokePoint>,
    val colorArgb: Int,
    val width: Float,
    val alpha: Float,
    val isEraser: Boolean = false,
)
