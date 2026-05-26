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
) : DrawRepositoryInterface {
    private val db = FirebaseFirestore.getInstance()
    private val snapshotListeners = mutableMapOf<String, ListenerRegistration>()
    private val roomConnectionStates = mutableMapOf<String, WebSocketConnectionState>()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isInternetAvailable = MutableStateFlow(value = true)
    override val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<WebSocketConnectionState> = _connectionState

    private val _debugLog = MutableStateFlow<List<String>>(emptyList())
    override val debugLog: StateFlow<List<String>> = _debugLog

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private fun addLog(message: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] $message"
        Log.d("DrawRepository", entry)
        _debugLog.value = (_debugLog.value + entry).takeLast(50)
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun recomputeAggregateConnectionState() {
        _connectionState.value = when {
            roomConnectionStates.values.any { it == WebSocketConnectionState.CONNECTED } ->
                WebSocketConnectionState.CONNECTED
            roomConnectionStates.values.any { it == WebSocketConnectionState.CONNECTING } ->
                WebSocketConnectionState.CONNECTING
            else -> WebSocketConnectionState.DISCONNECTED
        }
    }

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
            _isInternetAvailable.value =
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != false
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
                },
            )
        } catch (e: Exception) {
            Log.e("DrawRepository", "Failed to register network callback", e)
        }
    }

    override fun connectToRoom(inviteCode: String) {
        if (snapshotListeners.containsKey(inviteCode)) return

        addLog("Connecting to Firestore room: $inviteCode")
        roomConnectionStates[inviteCode] = WebSocketConnectionState.CONNECTING
        recomputeAggregateConnectionState()

        snapshotListeners[inviteCode] = db.collection("rooms").document(inviteCode)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    addLog("❌ Firestore Listen Error ($inviteCode): ${e.message}")
                    roomConnectionStates[inviteCode] = WebSocketConnectionState.DISCONNECTED
                    recomputeAggregateConnectionState()
                    return@addSnapshotListener
                }

                roomConnectionStates[inviteCode] = WebSocketConnectionState.CONNECTED
                recomputeAggregateConnectionState()

                snapshots?.documentChanges?.forEach { dc ->
                    when (dc.type) {
                        DocumentChange.Type.ADDED,
                        DocumentChange.Type.MODIFIED -> handleIncomingFirestoreMessage(inviteCode, dc.document.data)
                        DocumentChange.Type.REMOVED -> {
                            scope.launch {
                                drawingDao.deleteMessage(dc.document.id)
                                LatestDrawingWidget().updateAll(context)
                            }
                            addLog("🗑️ Drawing removed by partner ($inviteCode): ${dc.document.id}")
                        }
                    }
                }
            }
    }

    override fun disconnectFromRoom(inviteCode: String) {
        snapshotListeners.remove(inviteCode)?.remove()
        roomConnectionStates.remove(inviteCode)
        recomputeAggregateConnectionState()
        addLog("Disconnected from room: $inviteCode")
    }

    fun disconnectAll() {
        snapshotListeners.values.forEach { it.remove() }
        snapshotListeners.clear()
        roomConnectionStates.clear()
        recomputeAggregateConnectionState()
        addLog("Disconnected from all rooms")
    }

    override fun sendDrawing(inviteCode: String, strokes: List<DrawStroke>, senderName: String, text: String?) {
        if (!snapshotListeners.containsKey(inviteCode)) return
        val messageId = "msg_${UUID.randomUUID()}"
        val strokesJson = encodeStrokes(strokes)
        val encryptedStrokes = CryptoUtils.encrypt(strokesJson, inviteCode)
        val timestamp = System.currentTimeMillis()

        scope.launch {
            drawingDao.insertMessage(
                DrawingMessageDomain(
                    id = messageId,
                    inviteCode = inviteCode,
                    senderId = localDeviceId,
                    senderName = senderName,
                    strokesJson = strokesJson,
                    text = text,
                    timestamp = timestamp,
                    isReceived = false,
                    isConfirmedDelivered = false,
                )
            )
        }

        db.collection("rooms").document(inviteCode).collection("messages").document(messageId)
            .set(hashMapOf(
                "type" to "drawing",
                "id" to messageId,
                "sender" to localDeviceId,
                "senderName" to senderName,
                "strokesJson" to encryptedStrokes,
                "text" to text,
                "timestamp" to timestamp,
            ))
            .addOnSuccessListener { addLog("✅ Drawing message SAVED to cloud: $inviteCode") }
            .addOnFailureListener { e -> addLog("❌ Cloud SEND failure: ${e.message}") }
    }

    private fun handleIncomingFirestoreMessage(inviteCode: String, data: Map<String, Any?>) {
        val sender = data["sender"] as? String ?: return
        val type = data["type"] as? String ?: return
        val messageId = data["id"] as? String ?: return

        if (sender != localDeviceId) {
            if (type == "drawing") {
                val senderName = data["senderName"] as? String ?: "Partner"
                val encryptedStrokes = data["strokesJson"] as? String ?: ""
                val text = data["text"] as? String
                val strokesJson = CryptoUtils.decrypt(encryptedStrokes, inviteCode)
                val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()

                scope.launch {
                    drawingDao.insertMessage(
                        DrawingMessageDomain(
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
                    )
                    LatestDrawingWidget().updateAll(context)
                }

                db.collection("rooms").document(inviteCode).collection("messages").document(messageId)
                    .update("deliveredTo", com.google.firebase.firestore.FieldValue.arrayUnion(localDeviceId))
            }
        } else {
            val deliveredTo = data["deliveredTo"] as? List<*>
            if (!deliveredTo.isNullOrEmpty()) {
                scope.launch { drawingDao.confirmDelivery(messageId) }
            }
        }
    }

    override fun getMessagesForRoom(inviteCode: String) = drawingDao.getMessagesForRoom(inviteCode)

    override suspend fun clearHistory(inviteCode: String) {
        drawingDao.clearMessagesForRoom(inviteCode)
    }

    override fun deleteMessage(inviteCode: String, messageId: String) {
        scope.launch {
            drawingDao.deleteMessage(messageId)
            LatestDrawingWidget().updateAll(context)
        }
        db.collection("rooms").document(inviteCode).collection("messages").document(messageId)
            .delete()
            .addOnSuccessListener { addLog("🗑️ Drawing deleted from cloud ($inviteCode): $messageId") }
            .addOnFailureListener { e -> addLog("❌ Cloud DELETE failure: ${e.message}") }
    }
}
