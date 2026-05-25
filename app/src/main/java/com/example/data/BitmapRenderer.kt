package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

object BitmapRenderer {

    fun renderStrokesToBitmap(strokesJson: String, width: Int = 400, height: Int = 300): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val canvasBackground = 0xFFF9F9FB.toInt()
        canvas.drawColor(canvasBackground)

        val strokes = decodeStrokes(strokesJson)

        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
        }

        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue

            paint.color = if (stroke.isEraser) canvasBackground else stroke.colorArgb
            paint.strokeWidth = stroke.width * (width / 1000f).coerceAtLeast(1f)
            paint.alpha = if (stroke.isEraser) 255 else (stroke.alpha * 255).toInt()

            val path = Path()
            val first = stroke.points.first()
            path.moveTo(first.x * width, first.y * height)

            for (i in 1 until stroke.points.size) {
                val pt = stroke.points[i]
                path.lineTo(pt.x * width, pt.y * height)
            }
            canvas.drawPath(path, paint)
        }

        return bitmap
    }
}
