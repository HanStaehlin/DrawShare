package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DrawRepositoryInterface {
    val isInternetAvailable: StateFlow<Boolean>
    val connectionState: StateFlow<WebSocketConnectionState>
    val debugLog: StateFlow<List<String>>
    fun connectToRoom(inviteCode: String)
    fun disconnectFromRoom(inviteCode: String)
    fun sendDrawing(inviteCode: String, strokes: List<DrawStroke>, senderName: String, text: String?)
    fun getMessagesForRoom(inviteCode: String): Flow<List<DrawingMessageDomain>>
    suspend fun clearHistory(inviteCode: String)
    fun deleteMessage(inviteCode: String, messageId: String)
}
