package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drawing_messages")
data class DrawingMessage(
    @PrimaryKey val id: String,
    val inviteCode: String,
    val senderId: String,
    val strokesJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isReceived: Boolean,
    val isConfirmedDelivered: Boolean
)
