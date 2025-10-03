package com.ssafy.a705.chatSet.repo

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ssafy.a705.chatSet.ChatContracts
import com.ssafy.a705.chatSet.api.ChatApi
import com.ssafy.a705.chatSet.dto.*
import com.ssafy.a705.chatSet.ws.ChatStompClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val okHttp: OkHttpClient
) : ChatRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val msgEventAdapter = moshi.adapter(ChatMessageEvent::class.java)
    private val readAdapter     = moshi.adapter(ReadStatusUpdateRes::class.java)
    private val sendReqAdapter  = moshi.adapter(SendMessageReq::class.java)
    private val readReqAdapter  = moshi.adapter(ReadLogUpdateReq::class.java)

    private var stomp: ChatStompClient? = null

    /**
     * 서버가 {"message": "...", "data": {...}} envelope 로 내려주므로
     * 여기서 껍질을 벗겨 순수 DTO로 반환한다.
     * 200 + data!=null => data
     * 400              => "데이터 없음"으로 간주하여 빈 히스토리 반환
     * 그 외             => 에러
     */
    override suspend fun loadHistory(roomId: String, page: Int, size: Int): ChatHistoryRes {
        val resp = api.getHistoryEnvelope(roomId, page, size)
        return when {
            resp.isSuccessful -> {
                resp.body()?.data
                    ?: ChatHistoryRes(messages = emptyList(), membersList = emptyList(), page = page, size = size, total = 0)
            }
            resp.code() == 400 -> {
                // 방은 존재하나 히스토리 없음 같은 케이스
                ChatHistoryRes(messages = emptyList(), membersList = emptyList(), page = page, size = size, total = 0)
            }
            else -> error("history HTTP ${resp.code()}")
        }
    }

    override fun connectAndSubscribe(wsPrimary: String, wsFallback: String, roomId: String): Flow<Any> {
        val c = ChatStompClient(okHttp)
        stomp = c

        // 1차 연결
        c.connect(
            wsUrl = "wss://$wsPrimary",
            onConnected = {
                c.subscribe(ChatContracts.topicChat(roomId))
                c.subscribe(ChatContracts.topicRead(roomId))
            },
            onFailure = {
                c.connect(
                    wsUrl = "wss://$wsFallback",
                    onConnected = {
                        c.subscribe(ChatContracts.topicChat(roomId))
                        c.subscribe(ChatContracts.topicRead(roomId))
                    },
                    onFailure = { /* 최종 실패 핸들 */ }
                )
            }
        )

        // STOMP 프레임 스트림 → body 추출 → JSON 디코드
        return c.frames
            .map { raw ->
                if (raw.startsWith("MESSAGE")) {
                    val body = raw.substringAfter("\n\n").trimEnd('\u0000')
                    // 메시지 이벤트 → 실패하면 읽음 이벤트 → 그래도 실패면 그냥 body 문자열
                    msgEventAdapter.fromJson(body)
                        ?: readAdapter.fromJson(body)
                        ?: body
                } else {
                    raw
                }
            }
            // CONNECTED 프레임 같은 잡음을 UI로 올리지 않도록 필터
            .filter { it !is String || !it.startsWith("CONNECTED") }
            .catch { e -> emit(e) } // 스트림 상에서 예외를 그대로 이벤트로 흘려보내기
    }

    override fun sendMessage(req: SendMessageReq) {
        val body = sendReqAdapter.toJson(req)
        stomp?.send(ChatContracts.SEND_MESSAGE_DEST, body)
    }

    override fun sendRead(req: ReadLogUpdateReq) {
        val body = readReqAdapter.toJson(req)
        stomp?.send(ChatContracts.READ_MESSAGE_DEST, body)
    }

    override fun disconnect() {
        stomp?.disconnect()
        stomp = null
    }
}
