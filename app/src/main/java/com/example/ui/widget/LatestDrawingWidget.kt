package com.example.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.data.AppDatabase
import com.example.data.BitmapRenderer
import androidx.compose.ui.graphics.Color
import androidx.glance.text.FontWeight

class LatestDrawingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val latestMessage = db.drawingDao().getLatestReceivedMessage()

        provideContent {
            GlanceTheme {
                WidgetContent(latestMessage?.strokesJson)
            }
        }
    }

    @Composable
    private fun WidgetContent(strokesJson: String?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "LATEST FROM PARTNER",
                style = TextStyle(
                    color = ColorProvider(Color.Gray),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFFF9F9FB))
                    .cornerRadius(16.dp)
            ) {
                if (strokesJson != null) {
                    val bitmap = BitmapRenderer.renderStrokesToBitmap(strokesJson)
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = "Latest drawing",
                        modifier = GlanceModifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No drawings received",
                            style = TextStyle(color = ColorProvider(Color.LightGray))
                        )
                    }
                }
            }
        }
    }
}

class LatestDrawingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LatestDrawingWidget()
}
