package com.example.cpen321application.ui.live

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.BuildConfig
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

private const val GRID_SIZE = 16 // 16 x 16 = 256 cells
private const val IMAGE_GAP_MS = 3_000L
private val BACKOFF_SECONDS = listOf(1, 2, 5)

enum class ConnectionStatus { CONNECTING, LIVE, DISCONNECTED }

class LiveUpdatesViewModel : ViewModel() {
    val cells = mutableStateListOf<Color>().apply {
        repeat(GRID_SIZE * GRID_SIZE) { add(Color.LightGray) }
    }

    private val _status = MutableStateFlow(ConnectionStatus.CONNECTING)
    val status: StateFlow<ConnectionStatus> = _status

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var lastPixelAtMs = 0L
    private var retryAttempt = 0
    private var stopped = false

    init {
        connect()
    }

    private fun connect() {
        if (stopped) return
        _status.value = ConnectionStatus.CONNECTING

        val wsUrl = BuildConfig.API_BASE_URL
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
            .trimEnd('/') + "/ws/pixels"

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                retryAttempt = 0
                _status.value = ConnectionStatus.LIVE
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                viewModelScope.launch(Dispatchers.Main) { handlePixelMessage(text) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scheduleReconnect()
            }
        })
    }

    private fun handlePixelMessage(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val x = json.get("x")?.asInt ?: return
            val y = json.get("y")?.asInt ?: return
            val colorHex = json.get("color")?.asString ?: return
            if (x !in 0 until GRID_SIZE || y !in 0 until GRID_SIZE) return

            val now = System.currentTimeMillis()
            if (lastPixelAtMs != 0L && now - lastPixelAtMs >= IMAGE_GAP_MS) {
                clearGrid()
            }
            lastPixelAtMs = now

            val color = parseColor(colorHex) ?: return
            val index = y * GRID_SIZE + x
            if (index in cells.indices) {
                cells[index] = color
            }
        } catch (_: Exception) {
            // Ignore malformed messages.
        }
    }

    private fun parseColor(hex: String): Color? {
        return try {
            val normalized = if (hex.startsWith("#")) hex else "#$hex"
            Color(android.graphics.Color.parseColor(normalized))
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun clearGrid() {
        for (i in cells.indices) cells[i] = Color.LightGray
    }

    private fun scheduleReconnect() {
        if (stopped) return
        _status.value = ConnectionStatus.DISCONNECTED

        val delaySeconds = BACKOFF_SECONDS[retryAttempt.coerceAtMost(BACKOFF_SECONDS.lastIndex)]
        retryAttempt += 1

        viewModelScope.launch {
            kotlinx.coroutines.delay(delaySeconds * 1000L)
            if (!stopped) connect()
        }
    }

    override fun onCleared() {
        stopped = true
        webSocket?.close(1000, "ViewModel cleared")
        client.dispatcher.executorService.shutdown()
    }
}
