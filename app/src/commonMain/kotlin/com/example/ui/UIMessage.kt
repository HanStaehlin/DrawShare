package com.example.ui

import com.example.data.DrawStroke

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
