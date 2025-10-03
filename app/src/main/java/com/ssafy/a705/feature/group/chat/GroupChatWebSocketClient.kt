package com.ssafy.a705.feature.group.chat

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupChatWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) : WebSocketListener() {

    companion object {
        private const val TAG = "GroupChatWS"
        private const val BASE_WS = "wss://glml.store/ws/chat"
    }

    private val gson = Gson()
    private var ws: WebSocket? = null
    private var reconnectJob: Job? = null
    private var closedByUser = false
    private var currentRoomId: String? = null
    private var currentToken: String? = null

    // 수신 스트림
    private val _incomingMessages = MutableSharedFlow<ChatMessageEvent>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<ChatMessageEvent> = _incomingMessages

    private val _incomingReadStatus = MutableSharedFlow<ReadStatusEvent>(extraBufferCapacity = 64)
    val incomingReadStatus: SharedFlow<ReadStatusEvent> = _incomingReadStatus

    // 연결 상태
    private val _connectionState = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Idle)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    fun connect(token: String, roomId: String) {
        Log.d(TAG, "그룹 채팅 웹소켓 연결 시작: roomId=$roomId")
        closedByUser = false
        currentToken = token
        currentRoomId = roomId
        _connectionState.value = WebSocketConnectionState.Connecting
        
        val wsUrl = "$BASE_WS?token=Bearer $token"
        val req = Request.Builder()
            .url(wsUrl)
            .build()
        
        Log.d(TAG, "웹소켓 연결 시도: $wsUrl")
        ws = okHttpClient.newWebSocket(req, this)
    }

    fun close() {
        Log.d(TAG, "그룹 채팅 웹소켓 연결 종료")
        closedByUser = true
        reconnectJob?.cancel()
        ws?.close(1000, "client closing")
        ws = null
        currentRoomId = null
        currentToken = null
        _connectionState.value = WebSocketConnectionState.Closed(1000, "client closing")
    }

    fun sendMessage(tempId: String, roomId: String, content: String) {
        val payload = SendMessageRequest(tempId, roomId, content)
        val json = gson.toJson(payload)
        val stompFrame = "SEND\ndestination:/app/chat.sendMessage\n\n$json\u0000"
        ws?.send(stompFrame)
        Log.d(TAG, "메시지 전송: tempId=$tempId, roomId=$roomId, content=$content")
    }

    fun sendReadStatus(roomId: String, lastReadMessageId: String) {
        val payload = ReadStatusRequest(roomId, lastReadMessageId)
        val json = gson.toJson(payload)
        val stompFrame = "SEND\ndestination:/app/chat.readMessage\n\n$json\u0000"
        ws?.send(stompFrame)
        Log.d(TAG, "읽음 상태 전송: roomId=$roomId, messageId=$lastReadMessageId")
    }

    fun subscribeToTopics(roomId: String) {
        // 메시지 구독
        val messageSubFrame = "SUBSCRIBE\nid:msg-$roomId\ndestination:/topic/chat.$roomId\n\n\u0000"
        ws?.send(messageSubFrame)
        Log.d(TAG, "메시지 토픽 구독 요청: /topic/chat.$roomId")
        
        // 읽음 상태 구독
        val readSubFrame = "SUBSCRIBE\nid:read-$roomId\ndestination:/topic/read-status.$roomId\n\n\u0000"
        ws?.send(readSubFrame)
        Log.d(TAG, "읽음 상태 토픽 구독 요청: /topic/read-status.$roomId")
        
        Log.d(TAG, "토픽 구독 완료: roomId=$roomId")
    }

    override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
        Log.d(TAG, "웹소켓 연결 성공: ${response.code}")
        _connectionState.value = WebSocketConnectionState.Connected(BASE_WS)
        reconnectJob?.cancel()
        
        // STOMP CONNECT 프레임 전송
        val connectFrame = "CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\u0000"
        webSocket.send(connectFrame)
        Log.d(TAG, "STOMP CONNECT 프레임 전송")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "웹소켓 메시지 수신: $text")
        try {
            // STOMP 프레임 파싱
            val lines = text.split("\n")
            if (lines.size < 4) {
                Log.d(TAG, "STOMP 프레임이 너무 짧음: ${lines.size}줄")
                return
            }
            
            val command = lines[0]
            val headers = mutableMapOf<String, String>()
            var bodyStartIndex = -1
            
            // 헤더 파싱
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isEmpty()) {
                    bodyStartIndex = i + 1
                    break
                }
                if (line.contains(":")) {
                    val colonIndex = line.indexOf(":")
                    val key = line.substring(0, colonIndex)
                    val value = line.substring(colonIndex + 1)
                    headers[key] = value
                }
            }
            
            // 본문 파싱
            val body = if (bodyStartIndex >= 0 && bodyStartIndex < lines.size) {
                lines.subList(bodyStartIndex, lines.size).joinToString("\n").removeSuffix("\u0000")
            } else ""
            
            Log.d(TAG, "STOMP 파싱 결과 - command: $command, headers: $headers, body: $body")
            
            when (command) {
                "MESSAGE" -> {
                    val destination = headers["destination"] ?: ""
                    Log.d(TAG, "MESSAGE 수신 - destination: $destination")
                    when {
                        destination.startsWith("/topic/chat.") -> {
                            try {
                                val messageEvent = gson.fromJson(body, ChatMessageEvent::class.java)
                                Log.d(TAG, "채팅 메시지 이벤트 파싱 성공: messageId=${messageEvent.messageId}, tempId=${messageEvent.tempId}, senderId=${messageEvent.senderId}")
                                CoroutineScope(Dispatchers.IO).launch {
                                    _incomingMessages.emit(messageEvent)
                                    Log.d(TAG, "채팅 메시지 이벤트 emit 완료: messageId=${messageEvent.messageId}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "채팅 메시지 파싱 실패: $body", e)
                            }
                        }
                        destination.startsWith("/topic/read-status.") -> {
                            try {
                                val readEvent = gson.fromJson(body, ReadStatusEvent::class.java)
                                Log.d(TAG, "읽음 상태 이벤트 파싱 성공: messageId=${readEvent.messageId}, readCount=${readEvent.readCount}")
                                CoroutineScope(Dispatchers.IO).launch {
                                    _incomingReadStatus.emit(readEvent)
                                    Log.d(TAG, "읽음 상태 이벤트 emit 완료: messageId=${readEvent.messageId}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "읽음 상태 파싱 실패: $body", e)
                            }
                        }
                        else -> {
                            Log.d(TAG, "알 수 없는 destination: $destination")
                        }
                    }
                }
                "CONNECTED" -> {
                    Log.d(TAG, "STOMP 연결 성공")
                    // STOMP 연결 성공 후 토픽 구독
                    currentRoomId?.let { roomId ->
                        subscribeToTopics(roomId)
                    }
                }
                "ERROR" -> {
                    Log.e(TAG, "STOMP 에러: $body")
                }
                else -> {
                    Log.d(TAG, "알 수 없는 STOMP 명령: $command")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 파싱 실패: $text", e)
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "웹소켓 종료됨: code=$code, reason=$reason")
        _connectionState.value = WebSocketConnectionState.Closed(code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        Log.e(TAG, "웹소켓 연결 실패: ${t.message}", t)
        _connectionState.value = WebSocketConnectionState.Failed(t.message)
        
        if (closedByUser) return
        
        // 지수 백오프 재접속
        if (reconnectJob?.isActive == true) return
        
        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            var backoff = 2000L
            var retryCount = 0
            val maxRetries = 5
            
            while (!closedByUser && currentToken != null && currentRoomId != null && retryCount < maxRetries) {
                delay(backoff)
                retryCount++
                Log.d(TAG, "재접속 시도 ${retryCount}/${maxRetries} (backoff=${backoff}ms)")
                
                try {
                    connect(currentToken!!, currentRoomId!!)
                    // 연결 성공 시 루프 종료
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "재접속 시도 실패: ${e.message}")
                    if (retryCount >= maxRetries) {
                        Log.e(TAG, "최대 재시도 횟수 초과")
                        break
                    }
                }
                
                backoff = (backoff * 2).coerceAtMost(30_000L)
            }
        }
    }
}

// WebSocket 이벤트 DTOs
data class SendMessageRequest(
    val tempId: String,
    val roomId: String,
    val content: String
)

data class ChatMessageEvent(
    val tempId: String?,
    val roomId: String,
    val messageId: String,
    val senderId: String,
    val content: String,
    val timestamp: String,
    val status: String
)

data class ReadStatusEvent(
    val roomId: String,
    val messageId: String,
    val readCount: Int
)

sealed class WebSocketConnectionState {
    data object Idle : WebSocketConnectionState()
    data object Connecting : WebSocketConnectionState()
    data class Connected(val url: String) : WebSocketConnectionState()
    data class Closed(val code: Int, val reason: String?) : WebSocketConnectionState()
    data class Failed(val error: String?) : WebSocketConnectionState()
}
