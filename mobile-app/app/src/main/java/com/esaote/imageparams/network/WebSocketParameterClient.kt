package com.esaote.imageparams.network

import android.util.Log
import com.esaote.imageparams.model.MessageType
import com.esaote.imageparams.model.ParameterMessage
import com.esaote.imageparams.model.ParameterPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketParameterClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    private var webSocket: WebSocket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    var onParametersReceived: ((ParameterPayload) -> Unit)? = null

    fun connect(url: String) {
        disconnect()

        val request = Request.Builder().url(url).build()
        webSocket = httpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.i("WSClient", "onOpen")
                    _isConnected.value = true
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.i("WSClient", "onMessage: $text")
                    runCatching {
                        json.decodeFromString<ParameterMessage>(text)
                    }.onSuccess { message ->
                        if (message.type == MessageType.UPDATE || message.type == MessageType.SYNC) {
                            onParametersReceived?.invoke(message.payload)
                        }
                    }.onFailure { e ->
                        Log.e("WSClient", "decode failed", e)
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i("WSClient", "onClosed code=$code reason=$reason")
                    _isConnected.value = false
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("WSClient", "onFailure", t)
                    _isConnected.value = false
                }
            }
        )
    }

    fun sendUpdate(payload: ParameterPayload) {
        val message = ParameterMessage(type = MessageType.UPDATE, payload = payload)
        val text = json.encodeToString(message)
        Log.i("WSClient", "send: $text (ws=${webSocket != null})")
        webSocket?.send(text)
    }

    fun disconnect() {
        webSocket?.close(1000, "manual disconnect")
        webSocket = null
        _isConnected.value = false
    }
}
