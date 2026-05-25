package com.example.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

internal val appJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val strokesSerializer = ListSerializer(DrawStroke.serializer())

fun encodeStrokes(strokes: List<DrawStroke>): String =
    appJson.encodeToString(strokesSerializer, strokes)

fun decodeStrokes(json: String): List<DrawStroke> = try {
    appJson.decodeFromString(strokesSerializer, json)
} catch (_: Exception) {
    emptyList()
}
