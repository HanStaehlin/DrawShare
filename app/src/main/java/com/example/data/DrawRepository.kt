package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.UUID

class DrawRepository(
    private val context: Context,
    private val drawingDao: DrawingDao
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listAdapter = moshi.adapter<List<DrawStroke>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, DrawStroke::class.java)
    )

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isInternetAvailable = MutableStateFlow(true)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState

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
        } catch (e: Exception) {
            _isInternetAvailable.value = true
        }

        try {
            connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isInternetAvailable.value = true
                    currentInviteCode?.let {
                        connectToRoom(it)
                    }
                }

                override fun onLost(network: Network) {
                    Log.d("DrawRepository", "Network reported lost. Keeping active try.")
                }
            })
        } catch (e: Exception) {
            Log.e("DrawRepository", "Failed to register network callback", e)
        }
    }

    private fun scheduleReconnect() {
        val inviteCode = currentInviteCode ?: return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            kotlinx.coroutines.delay(3000)
            if (currentInviteCode == inviteCode && _connectionState.value != WebSocketConnectionState.CONNECTED) {
                Log.d("DrawRepository", "Attempting automatic reconnection for room: $inviteCode")
                connectToRoom(inviteCode)
            }
        }
    }

    fun connectToRoom(inviteCode: String) {
        currentInviteCode = inviteCode
        activeWebSocket?.close(1000, "Switching room")
        _connectionState.value = WebSocketConnectionState.CONNECTING
        reconnectJob?.cancel()

        // Allow WebSocket connection to attempt anyway. This makes the connection resilient
        // to platform-specific reporting issues on some Android ROMs/vendors.

        // We use the globally trusted public testing sandbox key from PieSocket
        val url = "wss://free.piesocket.com/v3/$inviteCode?api_key=VCbSZaNdaNrnAt66LDQu9M4tZ26PtoE0eR78vQO6"
        val request = Request.Builder().url(url).build()

        activeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = WebSocketConnectionState.CONNECTED
                Log.d("DrawRepository", "WebSocket connection opened in room: $inviteCode")
                reconnectJob?.cancel()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = WebSocketConnectionState.DISCONNECTED
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("DrawRepository", "WebSocket Failure: ${t.message}")
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

    fun sendDrawing(strokes: List<DrawStroke>) {
        val inviteCode = currentInviteCode ?: return
        val messageId = "msg_${UUID.randomUUID()}"
        val strokesJson = listAdapter.toJson(strokes) ?: "[]"

        val msg = DrawingMessage(
            id = messageId,
            inviteCode = inviteCode,
            senderId = localDeviceId,
            strokesJson = strokesJson,
            timestamp = System.currentTimeMillis(),
            isReceived = false,
            isConfirmedDelivered = false
        )

        scope.launch {
            drawingDao.insertMessage(msg)
        }

        // Broadcaster payload includes device ID and message ID
        val jsonPayload = JSONObject().apply {
            put("type", "drawing")
            put("id", messageId)
            put("sender", localDeviceId)
            put("strokesJson", strokesJson)
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
                    val strokesJson = json.optString("strokesJson")
                    val inviteCode = currentInviteCode ?: return

                    val incomingMsg = DrawingMessage(
                        id = messageId,
                        inviteCode = inviteCode,
                        senderId = sender,
                        strokesJson = strokesJson,
                        timestamp = System.currentTimeMillis(),
                        isReceived = true,
                        isConfirmedDelivered = true
                    )

                    scope.launch {
                        drawingDao.insertMessage(incomingMsg)
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
