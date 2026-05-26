package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AppDatabase
import com.example.data.DrawRepository
import com.example.data.SessionStore
import com.example.ui.DrawViewModel

class MainActivity : ComponentActivity() {

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            viewModelFactory {
                initializer {
                    val context = applicationContext
                    val db = AppDatabase.getDatabase(context)
                    DrawViewModel(
                        session = SessionStore(context),
                        repository = DrawRepository(context, db.drawingDao()),
                    )
                }
            }
        )[DrawViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color.White,
                    surface = Color.White,
                    primary = Color.Black,
                    onBackground = Color.Black,
                    onSurface = Color.Black,
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                ) {
                    DrawShareApp(viewModel = viewModel)
                }
            }
        }
    }
}
