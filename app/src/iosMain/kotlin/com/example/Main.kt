package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import com.example.data.AppDatabase
import com.example.data.IosDrawRepository
import com.example.data.SessionStore
import com.example.ui.DrawViewModel

private val appViewModel: DrawViewModel by lazy {
    val db = AppDatabase.getDatabase()
    DrawViewModel(
        session = SessionStore(),
        repository = IosDrawRepository(db.drawingDao()),
    )
}

fun MainViewController() = ComposeUIViewController {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = Color.White,
            surface = Color.White,
            primary = Color.Black,
            onBackground = Color.Black,
            onSurface = Color.Black,
        )
    ) {
        Surface(color = Color.White) {
            DrawShareApp(viewModel = appViewModel)
        }
    }
}
