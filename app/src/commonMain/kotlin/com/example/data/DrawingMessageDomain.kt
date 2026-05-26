package com.example.data

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
