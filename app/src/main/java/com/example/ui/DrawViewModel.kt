package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DrawViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = DrawRepository(application, db.drawingDao())

    val isInternetAvailable: StateFlow<Boolean> = repository.isInternetAvailable
    val connectionState: StateFlow<WebSocketConnectionState> = repository.connectionState
    val debugLog: StateFlow<List<String>> = repository.debugLog

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode

    // Draw active properties state (Color, Opacity Slider, Stroke Width)
    private val _selectedColor = MutableStateFlow(0xFF000000.toInt()) // Pure Black
    val selectedColor: StateFlow<Int> = _selectedColor

    private val _selectedAlpha = MutableStateFlow(1.0f) // Fully opaque default
    val selectedAlpha: StateFlow<Float> = _selectedAlpha

    private val _selectedWidth = MutableStateFlow(8.0f) // Default Brush size
    val selectedWidth: StateFlow<Float> = _selectedWidth

    // Direct thread-safe active canvas stokes path tracker
    val activeStrokes = mutableStateListOf<DrawStroke>()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listAdapter = moshi.adapter<List<DrawStroke>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, DrawStroke::class.java)
    )

    // FlatMap room session history to real-time UI render models
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val roomMessages: StateFlow<List<UIMessage>> = _inviteCode
        .flatMapLatest { code ->
            if (code == null) {
                flowOf(emptyList())
            } else {
                repository.getMessagesForRoom(code).map { entityList ->
                    entityList.map { entity ->
                        val strokes = try {
                            listAdapter.fromJson(entity.strokesJson) ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                        UIMessage(
                            id = entity.id,
                            senderId = entity.senderId,
                            isReceived = entity.isReceived,
                            isConfirmedDelivered = entity.isConfirmedDelivered,
                            timestamp = entity.timestamp,
                            strokes = strokes
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun generateInviteCode(): String {
        val code = (10000..99999).random().toString()
        joinRoom(code)
        return code
    }

    fun joinRoom(code: String) {
        val clean = code.trim()
        if (clean.isNotEmpty()) {
            _inviteCode.value = clean
            repository.connectToRoom(clean)
        }
    }

    fun disconnect() {
        _inviteCode.value = null
        repository.disconnect()
        activeStrokes.clear()
    }

    fun changeColor(colorArgb: Int) {
        _selectedColor.value = colorArgb
    }

    fun changeAlpha(alpha: Float) {
        _selectedAlpha.value = alpha
    }

    fun changeWidth(width: Float) {
        _selectedWidth.value = width
    }

    fun addStroke(stroke: DrawStroke) {
        activeStrokes.add(stroke)
    }

    fun undoLastStroke() {
        if (activeStrokes.isNotEmpty()) {
            activeStrokes.removeAt(activeStrokes.lastIndex)
        }
    }

    fun clearCanvas() {
        activeStrokes.clear()
    }

    fun sendCurrentDrawing() {
        if (activeStrokes.isEmpty()) return
        val currentSnap = activeStrokes.toList()
        repository.sendDrawing(currentSnap)
        activeStrokes.clear()
    }

    fun clearRoomHistory() {
        val code = _inviteCode.value ?: return
        viewModelScope.launch {
            repository.clearHistory(code)
        }
    }
}

data class UIMessage(
    val id: String,
    val senderId: String,
    val isReceived: Boolean,
    val isConfirmedDelivered: Boolean,
    val timestamp: Long,
    val strokes: List<DrawStroke>
)
