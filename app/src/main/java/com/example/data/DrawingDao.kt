package com.example.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// Domain model used by the rest of the app — keeps Boolean fields.
data class DrawingMessageDomain(
    val id: String,
    val inviteCode: String,
    val senderId: String,
    val senderName: String,
    val strokesJson: String,
    val text: String?,
    val timestamp: Long,
    val isReceived: Boolean,
    val isConfirmedDelivered: Boolean,
)

private fun DrawingMessage.toDomain() = DrawingMessageDomain(
    id = id,
    inviteCode = inviteCode,
    senderId = senderId,
    senderName = senderName,
    strokesJson = strokesJson,
    text = text,
    timestamp = timestamp,
    isReceived = isReceived != 0L,
    isConfirmedDelivered = isConfirmedDelivered != 0L,
)

class DrawingDao(db: DrawShareDb) {
    private val queries = db.drawingMessageQueries

    fun getMessagesForRoom(inviteCode: String): Flow<List<DrawingMessageDomain>> =
        queries.getMessagesForRoom(inviteCode).asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    suspend fun insertMessage(message: DrawingMessageDomain) = withContext(Dispatchers.IO) {
        queries.insertMessage(
            id = message.id,
            inviteCode = message.inviteCode,
            senderId = message.senderId,
            senderName = message.senderName,
            strokesJson = message.strokesJson,
            text = message.text,
            timestamp = message.timestamp,
            isReceived = if (message.isReceived) 1L else 0L,
            isConfirmedDelivered = if (message.isConfirmedDelivered) 1L else 0L,
        )
    }

    suspend fun confirmDelivery(id: String) = withContext(Dispatchers.IO) {
        queries.confirmDelivery(id)
    }

    suspend fun clearMessagesForRoom(inviteCode: String) = withContext(Dispatchers.IO) {
        queries.clearMessagesForRoom(inviteCode)
    }

    suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
        queries.deleteMessage(id)
    }

    suspend fun getLatestReceivedMessage(): DrawingMessageDomain? = withContext(Dispatchers.IO) {
        queries.getLatestReceivedMessage().executeAsOneOrNull()?.toDomain()
    }
}
