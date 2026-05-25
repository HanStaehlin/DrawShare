package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DrawViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = DrawRepository(application, db.drawingDao())
    private val session = SessionStore(application)

    val isInternetAvailable: StateFlow<Boolean> = repository.isInternetAvailable
    val connectionState: StateFlow<WebSocketConnectionState> = repository.connectionState
    val debugLog: StateFlow<List<String>> = repository.debugLog

    private val _joinedRooms = MutableStateFlow<List<String>>(emptyList())
    val joinedRooms: StateFlow<List<String>> = _joinedRooms

    private val _activeRoom = MutableStateFlow<String?>(null)
    val activeRoom: StateFlow<String?> = _activeRoom

    private val _userName = MutableStateFlow(session.userName ?: "User")
    val userName: StateFlow<String> = _userName

    init {
        val saved = session.joinedRooms
        if (saved.isNotEmpty()) {
            _joinedRooms.value = saved
            saved.forEach { repository.connectToRoom(it) }
            _activeRoom.value = session.activeRoom?.takeIf { it in saved } ?: saved.first()
        }
    }

    // Draw active properties state (Color, Opacity Slider, Stroke Width)
    private val _selectedColor = MutableStateFlow(0xFF000000.toInt()) // Pure Black
    val selectedColor: StateFlow<Int> = _selectedColor

    private val _selectedAlpha = MutableStateFlow(1.0f) // Fully opaque default
    val selectedAlpha: StateFlow<Float> = _selectedAlpha

    private val _selectedWidth = MutableStateFlow(8.0f) // Default Brush size
    val selectedWidth: StateFlow<Float> = _selectedWidth

    private val _isEraserMode = MutableStateFlow(value = false)
    val isEraserMode: StateFlow<Boolean> = _isEraserMode

    private val _currentMessageText = MutableStateFlow("")
    val currentMessageText: StateFlow<String> = _currentMessageText

    // Direct thread-safe active canvas stokes path tracker
    val activeStrokes = mutableStateListOf<DrawStroke>()

    // FlatMap room session history to real-time UI render models
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val roomMessages: StateFlow<List<UIMessage>> = _activeRoom
        .flatMapLatest { code ->
            if (code == null) {
                flowOf(emptyList())
            } else {
                repository.getMessagesForRoom(code).map { entityList ->
                    entityList.map { entity ->
                        val strokes = decodeStrokes(entity.strokesJson)
                        UIMessage(
                            id = entity.id,
                            senderId = entity.senderId,
                            senderName = entity.senderName,
                            isReceived = entity.isReceived,
                            isConfirmedDelivered = entity.isConfirmedDelivered,
                            timestamp = entity.timestamp,
                            text = entity.text,
                            strokes = strokes,
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
        if (clean.isEmpty()) return
        if (clean !in _joinedRooms.value) {
            _joinedRooms.value = _joinedRooms.value + clean
            session.joinedRooms = _joinedRooms.value
            repository.connectToRoom(clean)
        }
        switchToRoom(clean)
    }

    fun switchToRoom(code: String) {
        if (code !in _joinedRooms.value) return
        if (_activeRoom.value == code) return
        activeStrokes.clear()
        _currentMessageText.value = ""
        _activeRoom.value = code
        session.activeRoom = code
    }

    fun requestJoinNewRoom() {
        // Hide the active board so the connection screen takes over; rooms stay joined.
        _activeRoom.value = null
        session.activeRoom = null
        activeStrokes.clear()
        _currentMessageText.value = ""
    }

    fun cancelJoinNewRoom() {
        val rooms = _joinedRooms.value
        if (rooms.isEmpty()) return
        val target = rooms.first()
        _activeRoom.value = target
        session.activeRoom = target
    }

    fun leaveCurrentRoom() {
        val code = _activeRoom.value ?: return
        val remaining = _joinedRooms.value.filterNot { it == code }
        _joinedRooms.value = remaining
        session.joinedRooms = remaining
        repository.disconnectFromRoom(code)
        val next = remaining.firstOrNull()
        _activeRoom.value = next
        session.activeRoom = next
        activeStrokes.clear()
        _currentMessageText.value = ""
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

    fun toggleEraser(enabled: Boolean) {
        _isEraserMode.value = enabled
    }

    fun setMessageText(text: String) {
        _currentMessageText.value = text
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

    fun clearRoomHistory() {
        val code = _activeRoom.value ?: return
        viewModelScope.launch {
            repository.clearHistory(code)
        }
    }

    fun deleteMessage(id: String) {
        val code = _activeRoom.value ?: return
        repository.deleteMessage(code, id)
    }

    fun setUserName(name: String) {
        val trimmed = name.take(20)
        _userName.value = trimmed
        session.userName = trimmed
    }

    fun sendCurrentDrawing() {
        if (activeStrokes.isEmpty()) return
        val code = _activeRoom.value ?: return
        val currentSnap = activeStrokes.toList()
        repository.sendDrawing(code, currentSnap, _userName.value, _currentMessageText.value.ifBlank { null })
        activeStrokes.clear()
        _currentMessageText.value = ""
    }
}

data class UIMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val isReceived: Boolean,
    val isConfirmedDelivered: Boolean,
    val timestamp: Long,
    val text: String? = null,
    val strokes: List<DrawStroke>,
)
