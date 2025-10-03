package com.ssafy.a705.chatSet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.chatSet.dto.*
import com.ssafy.a705.chatSet.repo.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiMessage(
    val messageId: String,
    val senderId: String,          // 이메일
    val senderNickname: String?,   // 닉네임 (members 매핑)
    val content: String,
    val timestamp: String,         // "yyyy.MM.dd HH:mm:ss"
    val readCount: Int
)

data class ChatUiState(
    val roomId: String = "",
    val members: Map<String, String> = emptyMap(),  // email -> nickname
    val messages: List<ChatUiMessage> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 30,
    val total: Long = 0L,
    val connected: Boolean = false,
)

class ChatViewModel(
    private val repo: ChatRepository
) : ViewModel() {

    private var myEmail: String? = null

    fun setMyEmail(email: String) {
        myEmail = email
    }

    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private var readTicker: Job? = null
    private var streamJob: Job? = null

    /** 방 진입 시 1페이지 로드 */
    fun initRoom(roomId: String, initialPageSize: Int = 30) {
        _state.value = _state.value.copy(roomId = roomId, page = 0, pageSize = initialPageSize)
        loadHistory(reset = true)
    }

    /** STOMP/WebSocket 연결 및 구독 */
    fun connect(
        wsPrimary: String = "glml.store/ws/chat",
        wsFallback: String = "glml.store/chat"
    ) {
        val roomId = state.value.roomId
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            repo.connectAndSubscribe(wsPrimary, wsFallback, roomId).collect { any ->
                when (any) {
                    is ChatMessageEvent   -> onIncomingMessage(any)
                    is ReadStatusUpdateRes -> onReadUpdate(any)
                }
            }
        }
        startReadTicker()
        _state.value = _state.value.copy(connected = true)
    }

    /** 실시간 수신 메시지 -> UI로 반영 + 즉시 읽음 보고 */
    private fun onIncomingMessage(evt: ChatMessageEvent) {
        val nickname = state.value.members[evt.senderId]
        val ui = ChatUiMessage(
            messageId = evt.messageId,
            senderId = evt.senderId,
            senderNickname = nickname,
            content = evt.content,
            timestamp = evt.timestamp,
            readCount = 0
        )
        val isMine = myEmail?.let { evt.senderId.equals(it, ignoreCase = true) } == true
        if (!isMine) {
            viewModelScope.launch {
                sendReadNow()
            }
        }
    }

    /** 읽음 카운트 갱신 */
    private fun onReadUpdate(r: ReadStatusUpdateRes) {
        _state.value = state.value.copy(
            messages = state.value.messages.map {
                if (it.messageId == r.messageId) it.copy(readCount = r.readCount) else it
            }
        )
    }
    fun onConnected() {
        viewModelScope.launch { sendReadNow() }
    }


    /** 과거 히스토리 로드 (reset=true면 초기화) */
    fun loadHistory(reset: Boolean = false) {
        viewModelScope.launch {
            val s = state.value
            val page = if (reset) 0 else s.page + 1
            val res = repo.loadHistory(s.roomId, page, s.pageSize)

            // 이메일 -> 닉네임
            val members = res.membersList.associate { it.email to it.nickname }
            val mergedMembers = if (reset) members else s.members + members

            // DTO -> UI 모델
            val newItems = res.messages.map {
                ChatUiMessage(
                    messageId = it.messageId,
                    senderId = it.senderId,
                    senderNickname = mergedMembers[it.senderId],
                    content = it.content,
                    timestamp = it.timestamp,
                    readCount = it.readCount
                )
            }

            val merged = if (reset) newItems else s.messages + newItems
            _state.value = s.copy(
                members = mergedMembers,
                messages = merged,
                page = page,
                total = res.total.toLong()
            )
        }
    }

    /** 메시지 전송 */
    fun sendMessage(content: String) {
        val s = state.value
        val tempId = "tmp-" + System.currentTimeMillis()
        viewModelScope.launch {
            repo.sendMessage(SendMessageReq(tempId, s.roomId, content))
        }
    }

    /** 주기적 읽음 보고 */
    private fun startReadTicker() {
        readTicker?.cancel()
        readTicker = viewModelScope.launch {
            while (true) {
                delay(10_000)
                sendReadNow()
            }
        }
    }

    /** 가장 최신 메시지 기준 읽음 보고 */
    private suspend fun sendReadNow() {
        val lastId = state.value.messages.firstOrNull()?.messageId ?: return
        repo.sendRead(ReadLogUpdateReq(state.value.roomId, lastId))
    }

    override fun onCleared() {
        super.onCleared()
        readTicker?.cancel()
        streamJob?.cancel()
        repo.disconnect()
    }
    fun refreshHead() {
        viewModelScope.launch {
            val s = state.value
            if (s.roomId.isBlank()) return@launch
            // 0페이지(최신)만 가져온다
            val res = repo.loadHistory(s.roomId, 0, s.pageSize)

            // 이메일->닉네임 병합
            val nick = res.membersList.associate { it.email to it.nickname }
            val mergedMembers = s.members + nick

            // DTO->UI
            val head = res.messages.map {
                ChatUiMessage(
                    messageId = it.messageId,
                    senderId = it.senderId,
                    senderNickname = mergedMembers[it.senderId],
                    content = it.content,
                    timestamp = it.timestamp,
                    readCount = it.readCount
                )
            }

            // 이미 있는 메시지 제외하고 앞에만 붙이기
            val existingIds = s.messages.asSequence().map { it.messageId }.toHashSet()
            val newOnes = head.filter { it.messageId !in existingIds }

            if (newOnes.isNotEmpty()) {
                _state.value = s.copy(
                    members = mergedMembers,
                    messages = newOnes + s.messages
                )
            }
        }
    }
}
