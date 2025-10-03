package com.ssafy.a705.domain.group.latecheck

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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket 클라이언트 (OkHttp, STOMP 쓰지 않음)
 * - Raw Text WebSocket 사용
 * - 연결 후 즉시 groupId 바인딩
 * - 지수 백오프 재연결
 */
@Singleton
class LiveLocationWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) : WebSocketListener() {

    companion object {
        private const val TAG = "LiveLocationWS"
        private const val BASE_WS = "wss://glml.store/ws/live-location"
    }

    private val gson = Gson()
    private var ws: WebSocket? = null
    private var reconnectJob: Job? = null
    private var closedByUser = false

    // 수신 스트림
    private val _incoming = MutableSharedFlow<LiveLocationReceived>(extraBufferCapacity = 64)
    val incoming: SharedFlow<LiveLocationReceived> = _incoming

    // 연결 상태
    private val _connectionState = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Idle)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    fun connect(jwt: String) {
        Log.d(TAG, "=== 🔌 웹소켓 연결 시작 ===")
        Log.d(TAG, "🔑 JWT 토큰: ${jwt.take(20)}...")
        Log.d(TAG, "🌐 연결 URL: $BASE_WS")

        closedByUser = false
        _connectionState.value = WebSocketConnectionState.Connecting

        val req = Request.Builder()
            .url("$BASE_WS?accessToken=$jwt")
            .build()

        Log.d(TAG, "🔗 웹소켓 연결 시도: $BASE_WS?accessToken=${jwt.take(20)}...")
        ws = okHttpClient.newWebSocket(req, this)
        Log.d(TAG, "=== 🔌 웹소켓 연결 요청 완료 ===")
    }

    fun close() {
        Log.d(TAG, "웹소켓 연결 종료")
        closedByUser = true
        reconnectJob?.cancel()
        ws?.close(1000, "client closing")
        ws = null
        _connectionState.value = WebSocketConnectionState.Closed(1000, "client closing")
    }

    fun sendLocation(groupId: Long, lat: Double, lng: Double, timestamp: String = TimeFmt.nowKst()) {
        val payload = LiveLocationSend(groupId, lat, lng, timestamp)
        val json = gson.toJson(payload)

        Log.d(TAG, "=== 📤 위치 전송 시작 ===")
        Log.d(TAG, "🆔 그룹 ID: $groupId")
        Log.d(TAG, "📍 위도: $lat")
        Log.d(TAG, "📍 경도: $lng")
        Log.d(TAG, "⏰ 타임스탬프: $timestamp")
        Log.d(TAG, "📄 JSON 페이로드: $json")

        val success = ws?.send(json) ?: false
        if (success) {
            Log.d(TAG, "✅ 위치 전송 성공")
        } else {
            Log.e(TAG, "❌ 위치 전송 실패 - WebSocket이 연결되지 않음")
        }
        Log.d(TAG, "=== 📤 위치 전송 완료 ===")
    }

    override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
        Log.d(TAG, "=== ✅ 웹소켓 연결 성공 ===")
        Log.d(TAG, "📊 응답 코드: ${response.code}")
        Log.d(TAG, "📊 응답 메시지: ${response.message}")
        Log.d(TAG, "🔗 연결된 URL: ${webSocket.request().url}")
        _connectionState.value = WebSocketConnectionState.Connected(BASE_WS)
        reconnectJob?.cancel()
        Log.d(TAG, "=== ✅ 웹소켓 연결 완료 ===")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "=== 📡 웹소켓 메시지 수신 ===")
        Log.d(TAG, "📄 원본 메시지: $text")
        Log.d(TAG, "📏 메시지 길이: ${text.length}자")

        try {
            val location = gson.fromJson(text, LiveLocationReceived::class.java)
            Log.d(TAG, "✅ 위치 데이터 파싱 성공")
            Log.d(TAG, "📧 멤버 이메일: ${location.memberEmail}")
            Log.d(TAG, "📍 위도: ${location.latitude}")
            Log.d(TAG, "📍 경도: ${location.longitude}")
            Log.d(TAG, "💰 지각비: ${location.lateFee}원")
            Log.d(TAG, "⏰ 타임스탬프: ${location.timestamp}")
            Log.d(TAG, "🆔 그룹 ID: ${location.groupId}")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    _incoming.emit(location)
                    Log.d(TAG, "✅ 위치 데이터 emit 성공: ${location.memberEmail}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 위치 데이터 emit 실패: ${location.memberEmail}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 위치 데이터 파싱 실패", e)
            Log.e(TAG, "📄 파싱 실패한 메시지: $text")
        }
        Log.d(TAG, "=== 📡 웹소켓 메시지 처리 완료 ===")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "웹소켓 종료됨: code=$code, reason=$reason")
        _connectionState.value = WebSocketConnectionState.Closed(code, reason)
        // 서버가 모든 인원이 도착하면 닫을 수 있음 → UI에서 표시
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        Log.e(TAG, "웹소켓 연결 실패: ${t.message}", t)
        _connectionState.value = WebSocketConnectionState.Failed(t.message)

        if (closedByUser) return

        // 지수 백오프 재접속
        if (reconnectJob?.isActive == true) return

        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            var backoff = 2000L
            while (!closedByUser) {
                delay(backoff)
                Log.d(TAG, "재접속 시도 (backoff=${backoff}ms)")
                connect(webSocket.request().url.queryParameter("accessToken") ?: "")
                backoff = (backoff * 2).coerceAtMost(30_000L)
            }
        }
    }
}
