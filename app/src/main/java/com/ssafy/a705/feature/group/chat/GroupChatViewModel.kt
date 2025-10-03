package com.ssafy.a705.feature.group.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.group.common.model.ChatMessage
import com.ssafy.a705.group.common.model.MessageStatus
import com.ssafy.a705.group.common.util.GroupStatusUtil
import com.ssafy.a705.common.network.GroupApiService
import com.ssafy.a705.common.network.TokenManager
import com.ssafy.a705.common.network.sign.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    private val groupApiService: GroupApiService,
    private val groupChatRepository: GroupChatRepository,
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    companion object {
        private const val TAG = "GroupChatViewModel"
    }

    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

    private var currentGroupId: Long? = null
    private var currentRoomId: String? = null
    private var messageIdCounter = 0
    private var totalGroupMembers: Int = 0 // 전체 그룹 멤버 수
    
    // 현재 사용자의 이메일 정보
    private val currentUserEmail: String?
        get() = sessionManager.load()?.email

    init {
        // WebSocket 이벤트 수집
        viewModelScope.launch {
            groupChatRepository.incomingMessages.collect { messageEvent ->
                handleIncomingMessage(messageEvent)
            }
        }

        viewModelScope.launch {
            groupChatRepository.incomingReadStatus.collect { readEvent ->
                handleReadStatusUpdate(readEvent)
            }
        }

        viewModelScope.launch {
            groupChatRepository.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    connectionState = state
                )
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText
        if (text.isBlank()) return

        val tempId = generateTempId()
        val roomId = currentRoomId ?: return

        // 로컬에 pending 메시지 추가
        val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val pendingMessage = ChatMessage(
            messageId = "",
            sender = "나",
            message = text,
            isMine = true,
            timestamp = currentTime,
            totalMembers = totalGroupMembers,
            status = MessageStatus.SENDING,
            tempId = tempId
        )

        _uiState.value = _uiState.value.copy(
            messages = listOf(pendingMessage) + _uiState.value.messages,
            inputText = ""
        )

        // WebSocket으로 메시지 전송
        groupChatRepository.sendMessage(tempId, roomId, text)
        
        // 타임아웃 처리 (5초 후에도 응답이 없으면 실패로 처리)
        viewModelScope.launch {
            delay(5000)
            val messages = _uiState.value.messages.toMutableList()
            val pendingIndex = messages.indexOfFirst { it.tempId == tempId }
            if (pendingIndex != -1 && messages[pendingIndex].status == MessageStatus.SENDING) {
                messages[pendingIndex] = messages[pendingIndex].copy(status = MessageStatus.FAILED)
                _uiState.value = _uiState.value.copy(messages = messages)
                Log.w(TAG, "메시지 전송 타임아웃: tempId=$tempId")
            }
        }
    }

    fun loadGroupInfo(groupId: Long) {
        currentGroupId = groupId
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val groupInfo = groupApiService.getGroupInfo(groupId)
                groupInfo.data?.let { group ->
                    // 자동 상태 변경 적용
                    val updatedStatus = GroupStatusUtil.getAutoUpdatedStatus(
                        group.status,
                        group.startAt,
                        group.endAt
                    )
                    
                    val displayStatus = GroupStatusUtil.getDisplayStatus(updatedStatus)
                    
                    // 그룹 멤버 정보를 가져와서 실제 멤버 수 설정
                    try {
                        val membersResponse = groupApiService.getGroupMembers(groupId)
                        totalGroupMembers = membersResponse.data?.membersCount ?: 2
                        Log.d(TAG, "그룹 멤버 수: $totalGroupMembers")
                    } catch (e: Exception) {
                        Log.w(TAG, "멤버 수 조회 실패, 기본값 2 사용", e)
                        totalGroupMembers = 2
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        groupName = group.name,
                        status = displayStatus
                    )

                    // 그룹 정보에서 chatRoomId 가져오기
                    group.chatRoomId?.let { roomId ->
                        Log.d(TAG, "그룹에서 chatRoomId 가져옴: $roomId")
                        currentRoomId = roomId
                        connectToChatRoom()
                    } ?: run {
                        Log.w(TAG, "그룹에 chatRoomId가 없음: groupId=$groupId")
                        _uiState.value = _uiState.value.copy(
                            error = "그룹에 채팅방이 없습니다."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "그룹 정보 로드 실패", e)
                _uiState.value = _uiState.value.copy(
                    error = "그룹 정보를 불러올 수 없습니다."
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }



    private fun connectToChatRoom() {
        val roomId = currentRoomId ?: return
        val token = tokenManager.getServerAccessToken() ?: return

        viewModelScope.launch {
            try {
                // WebSocket 연결
                groupChatRepository.connectToChatRoom(token, roomId)
                
                // 히스토리 로드
                loadChatHistory(roomId, 0)
                
            } catch (e: Exception) {
                Log.e(TAG, "채팅방 연결 실패", e)
                _uiState.value = _uiState.value.copy(
                    error = "채팅방에 연결할 수 없습니다."
                )
            }
        }
    }

    private suspend fun loadChatHistory(roomId: String, page: Int) {
        try {
            val history = groupChatRepository.getChatHistory(roomId, page)
            
            // 멤버 닉네임 캐시 업데이트
            groupChatRepository.updateMemberNicknameCache(history.membersList)
            
                         // 메시지 변환
             val chatMessages = history.messages.map { messageDto ->
                 val nickname = groupChatRepository.getNicknameByEmail(messageDto.senderId)
                 val isMyMessage = messageDto.senderId == currentUserEmail
                 ChatMessage(
                     messageId = messageDto.messageId,
                     sender = nickname,
                     message = messageDto.content,
                     isMine = isMyMessage, // 현재 사용자 이메일과 비교
                     timestamp = messageDto.timestamp,
                     readCount = messageDto.readCount,
                     totalMembers = history.membersList.size // 실제 멤버 수 사용
                 )
             }.sortedByDescending { it.timestamp } // 최신순으로 정렬 (reverseLayout = true 사용)
            
            if (page == 0) {
                // 첫 페이지는 전체 교체
                _uiState.value = _uiState.value.copy(messages = chatMessages)
                
                // 읽음 상태 업데이트 시작
                chatMessages.lastOrNull()?.let { lastMessage ->
                    groupChatRepository.startReadStatusUpdates(roomId, lastMessage.messageId)
                }
                         } else {
                 // 추가 페이지는 앞에 추가 (무한 스크롤)
                 val existingMessages = _uiState.value.messages.toMutableList()
                 existingMessages.addAll(0, chatMessages)
                 // 전체 메시지를 최신순으로 정렬 (reverseLayout = true 사용)
                 val sortedMessages = existingMessages.sortedByDescending { it.timestamp }
                 _uiState.value = _uiState.value.copy(messages = sortedMessages)
             }
            
        } catch (e: Exception) {
            Log.e(TAG, "채팅 히스토리 로드 실패", e)
            // 히스토리 로드 실패해도 WebSocket 연결은 유지
            if (page == 0) {
                _uiState.value = _uiState.value.copy(
                    error = "채팅 기록을 불러올 수 없습니다. 실시간 메시지는 정상 작동합니다."
                )
            }
        }
    }

    private fun handleIncomingMessage(messageEvent: ChatMessageEvent) {
        Log.d(TAG, "메시지 이벤트 처리 시작: messageId=${messageEvent.messageId}, tempId=${messageEvent.tempId}, senderId=${messageEvent.senderId}")
        val messages = _uiState.value.messages.toMutableList()
        
        // 이미 처리된 메시지인지 확인 (messageId로 중복 체크)
        val existingIndex = messages.indexOfFirst { it.messageId == messageEvent.messageId }
        if (existingIndex != -1) {
            Log.d(TAG, "이미 처리된 메시지 무시: messageId=${messageEvent.messageId}, 기존 인덱스: $existingIndex")
            return
        }
        Log.d(TAG, "새로운 메시지 처리 시작: messageId=${messageEvent.messageId}")
        
        // tempId로 pending 메시지 찾아서 업데이트
        val pendingIndex = messages.indexOfFirst { it.tempId == messageEvent.tempId }
        
                 if (pendingIndex != -1) {
             // pending 메시지를 실제 메시지로 교체
             val nickname = groupChatRepository.getNicknameByEmail(messageEvent.senderId)
             val updatedMessage = ChatMessage(
                 messageId = messageEvent.messageId,
                 sender = nickname,
                 message = messageEvent.content,
                 isMine = true, // tempId가 있으면 내가 보낸 메시지
                 timestamp = messageEvent.timestamp,
                 totalMembers = totalGroupMembers,
                 status = MessageStatus.SENT
             )
             messages[pendingIndex] = updatedMessage
             Log.d(TAG, "Pending 메시지 업데이트: tempId=${messageEvent.tempId} -> messageId=${messageEvent.messageId}")
         } else {
             // 새로운 메시지 추가 (다른 사람이 보낸 메시지)
             val nickname = groupChatRepository.getNicknameByEmail(messageEvent.senderId)
             val isMyMessage = messageEvent.senderId == currentUserEmail
             val newMessage = ChatMessage(
                 messageId = messageEvent.messageId,
                 sender = nickname,
                 message = messageEvent.content,
                 isMine = isMyMessage, // senderId와 현재 사용자 이메일 비교
                 timestamp = messageEvent.timestamp,
                 totalMembers = totalGroupMembers,
                 status = MessageStatus.SENT
             )
             // 최신 메시지가 맨 앞에 오도록 삽입
             messages.add(0, newMessage)
             Log.d(TAG, "새 메시지 추가: ${newMessage.sender} - ${newMessage.message} (isMine: $isMyMessage)")
         }
        
        _uiState.value = _uiState.value.copy(messages = messages)
        
        // 읽음 상태 업데이트 트리거
        messageEvent.messageId.let { messageId ->
            currentRoomId?.let { roomId ->
                viewModelScope.launch {
                    groupChatRepository.updateReadStatus(roomId, messageId)
                }
            }
        }
    }

    private fun handleReadStatusUpdate(readEvent: ReadStatusEvent) {
        Log.d(TAG, "읽음 상태 이벤트 처리 시작: messageId=${readEvent.messageId}, readCount=${readEvent.readCount}")
        val messages = _uiState.value.messages.toMutableList()
        val messageIndex = messages.indexOfFirst { it.messageId == readEvent.messageId }
        
        if (messageIndex != -1) {
            val message = messages[messageIndex]
            messages[messageIndex] = message.copy(readCount = readEvent.readCount)
            _uiState.value = _uiState.value.copy(messages = messages)
        }
    }

    private fun generateTempId(): String {
        return "temp_${System.currentTimeMillis()}_${++messageIdCounter}"
    }
    
    /**
     * 읽지 않은 사람 수를 계산 (카톡처럼)
     * 전체 멤버 수에서 읽은 사람 수를 빼고, 현재 사용자는 제외
     */
    private fun calculateUnreadCount(readCount: Int, totalMembers: Int): Int {
        val currentUserIncluded = currentUserEmail != null
        return if (currentUserIncluded) {
            // 현재 사용자가 포함되어 있다면, 전체 멤버 수에서 읽은 사람 수를 빼고 현재 사용자도 제외
            maxOf(0, totalMembers - readCount - 1)
        } else {
            // 현재 사용자 정보가 없다면, 전체 멤버 수에서 읽은 사람 수를 뺌
            maxOf(0, totalMembers - readCount)
        }
    }

    fun loadMoreHistory() {
        val roomId = currentRoomId ?: return
        val currentPage = (_uiState.value.messages.size / 30) + 1
        
        viewModelScope.launch {
            loadChatHistory(roomId, currentPage)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        groupChatRepository.disconnectFromChatRoom()
    }
}
