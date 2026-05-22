package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.example.ui.widget.LatestDrawingWidget
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.UUID

class DrawRepository(
    private val context: Context,
    private val drawingDao: DrawingDao,
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listAdapter = moshi.adapter<List<DrawStroke>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, DrawStroke::class.java),
    )

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

    private var activeWebSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient.Builder().build()
    private var currentInviteCode: String? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: kotlinx.coroutines.Job? = null

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
                    // Only reconnect if we are actually disconnected.
                    // Don't tear down a healthy connection just because Android
                    // re-reported network availability.
                    if (_connectionState.value == WebSocketConnectionState.DISCONNECTED) {
                        addLog("Network available → auto-reconnecting")
                        currentInviteCode?.let {
                            connectToRoom(it)
                        }
                    } else {
                        addLog("Network available (already ${_connectionState.value}, skipping reconnect)")
                    }
                }

                override fun onLost(network: Network) {
                    addLog("Network reported lost")
                }
            }
            )
        } catch (e: Exception) {
            Log.e("DrawRepository", "Failed to register network callback", e)
        }
    }

    private fun scheduleReconnect() {
        val inviteCode = currentInviteCode ?: return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            kotlinx.coroutines.delay(3000)
            if ((currentInviteCode == inviteCode) && (_connectionState.value != WebSocketConnectionState.CONNECTED)) {
                addLog("Auto-reconnect triggered for room: $inviteCode")
                connectToRoom(inviteCode)
            }
        }
    }

    fun connectToRoom(inviteCode: String) {
        // Skip if already connected to this exact room
        if (inviteCode == currentInviteCode
            && _connectionState.value == WebSocketConnectionState.CONNECTED) {
            addLog("connectToRoom skipped (already connected to $inviteCode)")
            return
        }
        addLog("Connecting to room: $inviteCode")
        currentInviteCode = inviteCode
        activeWebSocket?.close(1000, "Switching room")
        _connectionState.value = WebSocketConnectionState.CONNECTING
        reconnectJob?.cancel()

        // We use the globally trusted public testing sandbox key from PieSocket
        val apiKey = com.example.BuildConfig.PIESOCKET_API_KEY
        val url = "wss://free.blr2.piesocket.com/v3/$inviteCode?api_key=$apiKey&notify_self=1"
        val request = Request.Builder().url(url).build()

        activeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = WebSocketConnectionState.CONNECTED
                addLog("✅ WebSocket CONNECTED to room: $inviteCode")
                reconnectJob?.cancel()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                addLog("WebSocket closing (code=$code, reason=$reason)")
                _connectionState.value = WebSocketConnectionState.DISCONNECTED
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                addLog("❌ WebSocket FAILURE: ${t.message}")
                _connectionState.value = WebSocketConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    fun disconnect() {
        currentInviteCode = null
        reconnectJob?.cancel()
        reconnectJob = null
        activeWebSocket?.close(1000, "Disconnect called")
        activeWebSocket = null
        _connectionState.value = WebSocketConnectionState.DISCONNECTED
    }

    fun sendDrawing(strokes: List<DrawStroke>, senderName: String) {
        val inviteCode = currentInviteCode ?: return
        val messageId = "msg_${UUID.randomUUID()}"
        val strokesJson = listAdapter.toJson(strokes) ?: "[]"

        val msg = DrawingMessage(
            id = messageId,
            inviteCode = inviteCode,
            senderId = localDeviceId,
            senderName = senderName,
            strokesJson = strokesJson,
            timestamp = System.currentTimeMillis(),
            isReceived = false,
            isConfirmedDelivered = false,
        )

        scope.launch {
            drawingDao.insertMessage(msg)
        }

        // Broadcaster payload includes device ID and message ID
        val encryptedStrokes = CryptoUtils.encrypt(strokesJson, inviteCode)

        val jsonPayload = JSONObject().apply {
            put("type", "drawing")
            put("id", messageId)
            put("sender", localDeviceId)
            put("senderName", senderName)
            put("strokesJson", encryptedStrokes)
        }

        val success = activeWebSocket?.send(jsonPayload.toString()) == true
        if (!success) {
            Log.e("DrawRepository", "Could not send message over WebSocket")
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            val sender = json.optString("sender")

            // Deduplicate self echo messages (PieSocket broadcasts to all connected clients in the same channel)
            if (sender == localDeviceId) return

            when (type) {
                "drawing" -> {
                    val messageId = json.optString("id")
                    val senderName = json.optString("senderName", "Partner")
                    val encryptedStrokes = json.optString("strokesJson")
                    val inviteCode = currentInviteCode ?: return
                    val strokesJson = CryptoUtils.decrypt(encryptedStrokes, inviteCode)

                    val incomingMsg = DrawingMessage(
                        id = messageId,
                        inviteCode = inviteCode,
                        senderId = sender,
                        senderName = senderName,
                        strokesJson = strokesJson,
                        timestamp = System.currentTimeMillis(),
                        isReceived = true,
                        isConfirmedDelivered = true,
                    )

                    scope.launch {
                        drawingDao.insertMessage(incomingMsg)
                        LatestDrawingWidget().updateAll(context)
                    }

                    // Acknowledge that we have received this message
                    val ackPayload = JSONObject().apply {
                        put("type", "ack")
                        put("id", messageId)
                        put("sender", localDeviceId)
                    }
                    activeWebSocket?.send(ackPayload.toString())
                }
                "ack" -> {
                    val messageId = json.optString("id")
                    scope.launch {
                        drawingDao.confirmDelivery(messageId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DrawRepository", "Error handling incoming message", e)
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
