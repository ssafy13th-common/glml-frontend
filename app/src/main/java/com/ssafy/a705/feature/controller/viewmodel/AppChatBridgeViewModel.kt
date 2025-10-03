package com.ssafy.a705.feature.controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.feature.chatSet.ChatViewModel as SetVM
import com.ssafy.a705.feature.chatSet.api.ChatApi
import com.ssafy.a705.feature.chatSet.dto.ChatMemberInfoDTO
import com.ssafy.a705.feature.chatSet.dto.ChatRoomRes
import com.ssafy.a705.feature.chatSet.dto.CreateRoomReq
import com.ssafy.a705.feature.chatSet.repo.ChatRepository
import com.ssafy.a705.feature.chatSet.dto.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppChatBridgeViewModel @Inject constructor(
    private val api: ChatApi,
    private val repo: ChatRepository,

) : ViewModel() {

    // chatSet 내부 VM
    private val setVm = SetVM(repo)

    // ===== 내 정보 =====
    private val _myEmail = MutableStateFlow("")
    val myEmail: StateFlow<String> = _myEmail
    val myId: StateFlow<Int> = _myEmail
        .map { it.hashCode() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setMyEmail(email: String) { _myEmail.value = email }

    // ===== 화면에서 바로 쓰는 메시지 리스트 (timestamp: Long) =====
    private val inFmt = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())

    val messages: StateFlow<List<ChatMessage>> =
        setVm.state
            .map { s ->
                s.messages.map { m ->
                    ChatMessage(
                        id = m.messageId.hashCode(),
                        senderId = m.senderId.hashCode(),
                        content = m.content,
                        // ✅ 문자열 타임스탬프 -> Long (nonnull 보장)
                        timestamp = runCatching { inFmt.parse(m.timestamp) }
                            .map { it.time }
                            .getOrElse { System.currentTimeMillis() }
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ===== 이메일해시 -> 닉네임 맵 =====
    val nickByHash: StateFlow<Map<Int, String>> =
        setVm.state
            .map { s -> s.members.mapKeys { (email, _) -> email.hashCode() } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // ===== 메시지ID해시 -> '읽은 수'(readCount) 맵 =====
    //   화면에서는 안읽음 수 = 참여자수(nickByHash.size) - 읽은 수 로 계산해 표시
    val readByMsgId: StateFlow<Map<Int, Int>> =
        setVm.state
            .map { s -> s.messages.associate { it.messageId.hashCode() to it.readCount } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // ===== 화면 수명주기 연동 =====
    // sendMessage와 enterRoom에 '짧은 지연 후 헤드 갱신' 추가
    fun enterRoom(roomId: String, pageSize: Int = 30) {
        setVm.initRoom(roomId, pageSize)
        setVm.connect()

        // ✅ 내 이메일을 ChatViewModel로 주입
        val email = _myEmail.value
        if (!email.isNullOrBlank()) {
            setVm.setMyEmail(email)
        }

        startAutoRefresh(3000)

        viewModelScope.launch {
            delay(250)   // 서버 readCount 반영 대기 (기존 로직 유지)
            setVm.refreshHead()             // 최신 헤드 싱크 (기존 로직 유지)
            // ✅ 연결 직후 1회 커서 보고(선택이지만 추천)

        }
    }

    fun sendMessage(text: String) {
        setVm.sendMessage(text)
        viewModelScope.launch {
            delay(200)
            setVm.refreshHead()
        }
    }
    //과거로드
    fun loadOlder() = setVm.loadHistory(reset = false)
    fun disconnect() {
        // onCleared()는 protected — 레포 경유로 끊기
        stopAutoRefresh()
        repo.disconnect()
    }

    // ===== DM 방 보장 후 콜백 (원래 있던 함수 그대로 유지) =====
    /**
     * 1) /chat/rooms/private?email= 으로 단건 조회 (없으면 400)
     * 2) 없으면 /chat/rooms 목록(없으면 400 → 빈 리스트)에서 1:1 방 탐색
     * 3) 그래도 없으면 /chat/rooms 로 방 생성
     * 4) onSuccess(roomId, title) 호출
     */
    fun ensureRoomAnd(
        requesterEmail: String,
        requesterNickname: String,
        targetEmail: String,
        targetNickname: String,
        roomName: String,
        onSuccess: (roomId: String, title: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                // 1) 단건 DM 조회
                val dmResp = api.getPrivateRoomResp(targetEmail)
                val dmRoom: ChatRoomRes? = when {
                    dmResp.isSuccessful -> dmResp.body()?.data
                    dmResp.code() == 400 -> null                         // 존재하지 않음
                    else -> error("private HTTP ${dmResp.code()}")
                }

                // 2) 목록에서 1:1 방 찾기
                val existing = dmRoom ?: run {
                    val roomsResp = api.getRoomsResp()
                    val rooms = when {
                        roomsResp.isSuccessful -> roomsResp.body()?.data?.rooms ?: emptyList()
                        roomsResp.code() == 400 -> emptyList()
                        else -> error("rooms HTTP ${roomsResp.code()}")
                    }
                    rooms.firstOrNull { room ->
                        val emails = room.participants().map { it.email }.toSet()
                        requesterEmail in emails && targetEmail in emails && emails.size == 2
                    }
                }

                // 3) 없으면 생성
                existing ?: run {
                    val createResp = api.createRoomEnvelope(
                        CreateRoomReq(
                            name = roomName,
                            membersInfo = listOf(
                                ChatMemberInfoDTO(requesterEmail, requesterNickname),
                                ChatMemberInfoDTO(targetEmail,   targetNickname)
                            )
                        )
                    )
                    if (!createResp.isSuccessful) error("create HTTP ${createResp.code()}")
                    createResp.body()?.data ?: error("createRoom: empty data")
                }
            }.onSuccess { room ->
                onSuccess(room.roomId, room.name)
            }.onFailure(onError)
        }
    }
    private var autoJob: Job? = null
    /** 보낸 직후 1회(300~500ms) 헤드 리프레시 */
    fun refreshAfterSend(delayMs: Long = 400) {
        autoJob?.cancel()
        autoJob = viewModelScope.launch {
            delay(delayMs)
            setVm.refreshHead()
        }
    }

    /** 주기적 헤드 리프레시 (헬스 체크용; 3~5초 권장) */
    fun startAutoRefresh(intervalMs: Long = 3000) {
        autoJob?.cancel()
        autoJob = viewModelScope.launch {
            while (true) {
                delay(intervalMs)
                setVm.refreshHead()
            }
        }
    }

    fun stopAutoRefresh() { autoJob?.cancel() }
}
