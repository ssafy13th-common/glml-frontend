package com.ssafy.a705.chatSet.ws

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class ChatStompClient(private val httpClient: OkHttpClient) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val _frames = Channel<String>(Channel.BUFFERED)
    val frames: Flow<String> = _frames.receiveAsFlow()

    fun connect(wsUrl: String, onConnected: (() -> Unit)? = null, onFailure: ((Throwable) -> Unit)? = null) {
        val req = Request.Builder().url(wsUrl).build() // Authorization 헤더는 앱의 Interceptor가 부착
        val client = httpClient.newBuilder().pingInterval(25, TimeUnit.SECONDS).build()
        webSocket = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val frame = "CONNECT\naccept-version:1.2\n\n\u0000"
                webSocket.send(frame)
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch { _frames.send(text) }
                if (text.startsWith("CONNECTED")) onConnected?.invoke()
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                scope.launch { _frames.send(bytes.utf8()) }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, r: Response?) {
                onFailure?.invoke(t)
            }
        })
    }

    fun subscribe(dest: String, id: String = "sub-${System.currentTimeMillis()}") {
        webSocket?.send("SUBSCRIBE\nid:$id\ndestination:$dest\n\n\u0000")
    }
    fun send(dest: String, body: String) {
        webSocket?.send("SEND\ndestination:$dest\ncontent-type:application/json\ncontent-length:${body.toByteArray().size}\n\n$body\u0000")
    }
    fun disconnect() { try { webSocket?.close(1000, "bye") } catch (_: Throwable) {}; webSocket = null }
}
