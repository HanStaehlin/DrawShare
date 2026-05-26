package com.example.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DrawViewModel(
    private val session: SessionStore,
    private val repository: DrawRepositoryInterface,
) : ViewModel() {

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

    private val _selectedColor = MutableStateFlow(0xFF000000.toInt())
    val selectedColor: StateFlow<Int> = _selectedColor

    private val _selectedAlpha = MutableStateFlow(1.0f)
    val selectedAlpha: StateFlow<Float> = _selectedAlpha

    private val _selectedWidth = MutableStateFlow(8.0f)
    val selectedWidth: StateFlow<Float> = _selectedWidth

    private val _isEraserMode = MutableStateFlow(false)
    val isEraserMode: StateFlow<Boolean> = _isEraserMode

    private val _currentMessageText = MutableStateFlow("")
    val currentMessageText: StateFlow<String> = _currentMessageText

    val activeStrokes = mutableStateListOf<DrawStroke>()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val roomMessages: StateFlow<List<UIMessage>> = _activeRoom
        .flatMapLatest { code ->
            if (code == null) flowOf(emptyList())
            else repository.getMessagesForRoom(code).map { entityList ->
                entityList.map { entity ->
                    UIMessage(
                        id = entity.id,
                        senderId = entity.senderId,
                        senderName = entity.senderName,
                        isReceived = entity.isReceived,
                        isConfirmedDelivered = entity.isConfirmedDelivered,
                        timestamp = entity.timestamp,
                        text = entity.text,
                        strokes = decodeStrokes(entity.strokesJson),
                    )
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
        _activeRoom.value = null
        session.activeRoom = null
        activeStrokes.clear()
        _currentMessageText.value = ""
    }

    fun cancelJoinNewRoom() {
        val rooms = _joinedRooms.value
        if (rooms.isEmpty()) return
        _activeRoom.value = rooms.first()
        session.activeRoom = rooms.first()
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

    fun changeColor(colorArgb: Int) { _selectedColor.value = colorArgb }
    fun changeAlpha(alpha: Float) { _selectedAlpha.value = alpha }
    fun changeWidth(width: Float) { _selectedWidth.value = width }
    fun toggleEraser(enabled: Boolean) { _isEraserMode.value = enabled }
    fun setMessageText(text: String) { _currentMessageText.value = text }
    fun addStroke(stroke: DrawStroke) { activeStrokes.add(stroke) }
    fun undoLastStroke() { if (activeStrokes.isNotEmpty()) activeStrokes.removeAt(activeStrokes.lastIndex) }
    fun clearCanvas() { activeStrokes.clear() }

    fun clearRoomHistory() {
        val code = _activeRoom.value ?: return
        viewModelScope.launch { repository.clearHistory(code) }
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
        val snap = activeStrokes.toList()
        repository.sendDrawing(code, snap, _userName.value, _currentMessageText.value.ifBlank { null })
        activeStrokes.clear()
        _currentMessageText.value = ""
    }
}
