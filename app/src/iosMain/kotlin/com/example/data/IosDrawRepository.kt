package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

// Stub iOS repository — Firebase integration via GitLive will be added once
// GitLive publishes a release compatible with Firebase iOS SDK / BOM 34.x.
class IosDrawRepository(private val drawingDao: DrawingDao) : DrawRepositoryInterface {

    private val _isInternetAvailable = MutableStateFlow(true)
    override val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<WebSocketConnectionState> = _connectionState

    private val _debugLog = MutableStateFlow<List<String>>(emptyList())
    override val debugLog: StateFlow<List<String>> = _debugLog

    override fun connectToRoom(inviteCode: String) {
        _connectionState.value = WebSocketConnectionState.CONNECTED
    }

    override fun disconnectFromRoom(inviteCode: String) {
        _connectionState.value = WebSocketConnectionState.DISCONNECTED
    }

    override fun sendDrawing(inviteCode: String, strokes: List<DrawStroke>, senderName: String, text: String?) {
        // No-op until GitLive Firebase is wired up
    }

    override fun getMessagesForRoom(inviteCode: String): Flow<List<DrawingMessageDomain>> =
        drawingDao.getMessagesForRoom(inviteCode)

    override suspend fun clearHistory(inviteCode: String) {
        drawingDao.clearMessagesForRoom(inviteCode)
    }

    override fun deleteMessage(inviteCode: String, messageId: String) {
        // no-op until GitLive Firebase is wired up
    }
}
