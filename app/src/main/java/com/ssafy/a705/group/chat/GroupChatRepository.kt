package com.ssafy.a705.group.chat

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupChatRepository @Inject constructor(
    private val groupChatApi: GroupChatApi,
    private val webSocketClient: GroupChatWebSocketClient
) {
    companion object {
        private const val TAG = "GroupChatRepo"
    }

    private var readStatusJob: Job? = null
    private var lastReadMessageId: String? = null
    private var currentRoomId: String? = null

    // 멤버 닉네임 매핑 캐시
    private val memberNicknameCache = mutableMapOf<String, String>()

    // 채팅방 목록 조회
    suspend fun getChatRooms(): ChatRoomsResponse {
        return try {
            Log.d(TAG, "채팅방 목록 조회 시작")
            val response = groupChatApi.getChatRooms()
            Log.d(TAG, "채팅방 목록 조회 응답: $response")
            response.data ?: throw Exception("채팅방 목록을 불러올 수 없습니다.")
        } catch (e: Exception) {
            Log.e(TAG, "채팅방 목록 조회 실패", e)
            throw e
        }
    }

    // 채팅 히스토리 조회
    suspend fun getChatHistory(roomId: String, page: Int, size: Int = 30): ChatHistoryResponse {
        return try {
            val response = groupChatApi.getChatHistory(roomId, page, size)
            response.data ?: throw Exception("채팅 히스토리를 불러올 수 없습니다.")
        } catch (e: Exception) {
            Log.e(TAG, "채팅 히스토리 조회 실패: roomId=$roomId, page=$page", e)
            throw e
        }
    }

    // WebSocket 연결 및 구독
    fun connectToChatRoom(token: String, roomId: String) {
        Log.d(TAG, "채팅방 연결: roomId=$roomId")
        currentRoomId = roomId
        webSocketClient.connect(token, roomId)
    }

    // WebSocket 연결 해제
    fun disconnectFromChatRoom() {
        Log.d(TAG, "채팅방 연결 해제")
        currentRoomId = null
        lastReadMessageId = null
        readStatusJob?.cancel()
        webSocketClient.close()
    }

    // 메시지 전송
    fun sendMessage(tempId: String, roomId: String, content: String) {
        webSocketClient.sendMessage(tempId, roomId, content)
    }

    // 읽음 상태 업데이트 (REST API 사용)
    suspend fun updateReadStatus(roomId: String, lastReadMessageId: String) {
        try {
            val request = ReadStatusRequest(roomId, lastReadMessageId)
            groupChatApi.updateReadStatus(roomId, request)
            Log.d(TAG, "읽음 상태 업데이트 성공: roomId=$roomId, messageId=$lastReadMessageId")
        } catch (e: Exception) {
            Log.e(TAG, "읽음 상태 업데이트 실패: roomId=$roomId, messageId=$lastReadMessageId", e)
        }
    }

    // 읽음 상태 주기적 업데이트 시작
    fun startReadStatusUpdates(roomId: String, lastMessageId: String) {
        lastReadMessageId = lastMessageId
        readStatusJob?.cancel()
        
        readStatusJob = CoroutineScope(Dispatchers.IO).launch {
            while (currentRoomId == roomId) {
                try {
                    updateReadStatus(roomId, lastMessageId)
                    delay(10000) // 10초마다
                } catch (e: Exception) {
                    Log.e(TAG, "읽음 상태 업데이트 실패", e)
                    delay(5000) // 에러 시 5초 후 재시도
                }
            }
        }
    }

    // 읽음 상태 업데이트 중지
    fun stopReadStatusUpdates() {
        readStatusJob?.cancel()
        readStatusJob = null
    }

    // 멤버 닉네임 캐시 업데이트
    fun updateMemberNicknameCache(membersList: List<ChatMemberDto>) {
        membersList.forEach { member ->
            memberNicknameCache[member.email] = member.nickname
        }
        Log.d(TAG, "멤버 닉네임 캐시 업데이트: ${memberNicknameCache.size}명")
    }

    // 멤버 닉네임 캐시 업데이트 (Map 형태)
    fun updateMemberNicknameCacheFromMap(membersIdMap: Map<String, String>) {
        membersIdMap.forEach { (email, nickname) ->
            memberNicknameCache[email] = nickname
        }
        Log.d(TAG, "멤버 닉네임 캐시 업데이트 (Map): ${membersIdMap.size}명")
    }

    // 이메일로 닉네임 조회
    fun getNicknameByEmail(email: String): String {
        return memberNicknameCache[email] ?: email.substringBefore("@")
    }

    // WebSocket 이벤트 스트림
    val incomingMessages: SharedFlow<ChatMessageEvent> = webSocketClient.incomingMessages
    val incomingReadStatus: SharedFlow<ReadStatusEvent> = webSocketClient.incomingReadStatus
    val connectionState: StateFlow<WebSocketConnectionState> = webSocketClient.connectionState

    // 현재 연결된 방 ID
    fun getCurrentRoomId(): String? = currentRoomId

    // 마지막 읽은 메시지 ID
    fun getLastReadMessageId(): String? = lastReadMessageId
}
