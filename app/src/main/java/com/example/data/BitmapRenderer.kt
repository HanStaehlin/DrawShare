package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object BitmapRenderer {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listAdapter = moshi.adapter<List<DrawStroke>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, DrawStroke::class.java),
    )

    fun renderStrokesToBitmap(strokesJson: String, width: Int = 400, height: Int = 300): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFFF9F9FB.toInt()) // Match canvas background color

        val strokes = try {
            listAdapter.fromJson(strokesJson) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
        }

        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            
            paint.color = stroke.colorArgb
            paint.strokeWidth = stroke.width * (width / 400f).coerceAtLeast(1f)
            paint.alpha = (stroke.alpha * 255).toInt()

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
