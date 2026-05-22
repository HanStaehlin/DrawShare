package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.example.ui.widget.LatestDrawingWidget
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DrawRepository(
    private val context: Context,
    private val drawingDao: DrawingDao,
) {
    private val db = FirebaseFirestore.getInstance()
    private var snapshotListener: ListenerRegistration? = null

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isInternetAvailable = MutableStateFlow(value = true)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState

    private val _debugLog = MutableStateFlow<List<String>>(emptyList())
    val debugLog: StateFlow<List<String>> = _debugLog

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private fun addLog(message: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] $message"
        Log.d("DrawRepository", entry)
        _debugLog.value = (_debugLog.value + entry).takeLast(50)
    }

    private var currentInviteCode: String? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    val localDeviceId: String = UUID.randomUUID().toString()

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            _isInternetAvailable.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != false
        } catch (_: Exception) {
            _isInternetAvailable.value = true
        }

        try {
            connectivityManager.registerNetworkCallback(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isInternetAvailable.value = true
                        addLog("Network available")
                    }

                    override fun onLost(network: Network) {
                        _isInternetAvailable.value = false
                        addLog("Network reported lost")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("DrawRepository", "Failed to register network callback", e)
        }
    }

    fun connectToRoom(inviteCode: String) {
        if (inviteCode == currentInviteCode && snapshotListener != null) {
            return
        }

        addLog("Connecting to Firestore room: $inviteCode")
        currentInviteCode = inviteCode
        
        // Stop any previous listener
        snapshotListener?.remove()
        _connectionState.value = WebSocketConnectionState.CONNECTING

        val roomRef = db.collection("rooms").document(inviteCode).collection("messages")
        
        // Listen for new messages
        snapshotListener = roomRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    addLog("❌ Firestore Listen Error: ${e.message}")
                    _connectionState.value = WebSocketConnectionState.DISCONNECTED
                    return@addSnapshotListener
                }

                _connectionState.value = WebSocketConnectionState.CONNECTED
                
                if (snapshots != null) {
                    for (dc in snapshots.documentChanges) {
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                handleIncomingFirestoreMessage(dc.document.data)
                            }
                            DocumentChange.Type.MODIFIED -> {
                                handleIncomingFirestoreMessage(dc.document.data)
                            }
                            else -> {}
                        }
                    }
                }
            }
    }

    fun disconnect() {
        currentInviteCode = null
        snapshotListener?.remove()
        snapshotListener = null
        _connectionState.value = WebSocketConnectionState.DISCONNECTED
        addLog("Disconnected from room")
    }

    fun sendDrawing(strokes: List<DrawStroke>, senderName: String, text: String? = null) {
        val inviteCode = currentInviteCode ?: return
        val messageId = "msg_${UUID.randomUUID()}"
        
        val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter<List<DrawStroke>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, DrawStroke::class.java))
        val strokesJson = adapter.toJson(strokes) ?: "[]"

        val msg = DrawingMessage(
            id = messageId,
            inviteCode = inviteCode,
            senderId = localDeviceId,
            senderName = senderName,
            strokesJson = strokesJson,
            text = text,
            timestamp = System.currentTimeMillis(),
            isReceived = false,
            isConfirmedDelivered = false,
        )

        scope.launch {
            drawingDao.insertMessage(msg)
        }

        val encryptedStrokes = CryptoUtils.encrypt(strokesJson, inviteCode)

        val data = hashMapOf(
            "type" to "drawing",
            "id" to messageId,
            "sender" to localDeviceId,
            "senderName" to senderName,
            "strokesJson" to encryptedStrokes,
            "text" to text,
            "timestamp" to msg.timestamp
        )

        db.collection("rooms").document(inviteCode).collection("messages").document(messageId)
            .set(data)
            .addOnSuccessListener {
                addLog("✅ Drawing message SAVED to cloud: $inviteCode")
            }
            .addOnFailureListener { e ->
                addLog("❌ Cloud SEND failure: ${e.message}")
            }
    }

    private fun handleIncomingFirestoreMessage(data: Map<String, Any?>) {
        val sender = data["sender"] as? String ?: return
        val type = data["type"] as? String ?: return
        val messageId = data["id"] as? String ?: return

        if (sender != localDeviceId) {
            when (type) {
                "drawing" -> {
                    val inviteCode = currentInviteCode ?: return
                    val senderName = data["senderName"] as? String ?: "Partner"
                    val encryptedStrokes = data["strokesJson"] as? String ?: ""
                    val text = data["text"] as? String
                    val strokesJson = CryptoUtils.decrypt(encryptedStrokes, inviteCode)
                    val timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis()

                    val incomingMsg = DrawingMessage(
                        id = messageId,
                        inviteCode = inviteCode,
                        senderId = sender,
                        senderName = senderName,
                        strokesJson = strokesJson,
                        text = text,
                        timestamp = timestamp,
                        isReceived = true,
                        isConfirmedDelivered = true,
                    )

                    scope.launch {
                        drawingDao.insertMessage(incomingMsg)
                        LatestDrawingWidget().updateAll(context)
                    }

                    // Acknowledge by updating the document in Firestore
                    db.collection("rooms").document(inviteCode).collection("messages").document(messageId)
                        .update("deliveredTo", com.google.firebase.firestore.FieldValue.arrayUnion(localDeviceId))
                }
            }
        } else {
            // Check for delivery confirmation on our own messages
            val deliveredTo = data["deliveredTo"] as? List<*>
            if (!deliveredTo.isNullOrEmpty()) {
                scope.launch {
                    drawingDao.confirmDelivery(messageId)
                }
            }
        }
    }

    fun getMessagesForRoom(inviteCode: String) = drawingDao.getMessagesForRoom(inviteCode)

    suspend fun clearHistory(inviteCode: String) {
        drawingDao.clearMessagesForRoom(inviteCode)
    }
}

enum class WebSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}
