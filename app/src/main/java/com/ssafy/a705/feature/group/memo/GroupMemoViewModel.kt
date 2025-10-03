package com.ssafy.a705.feature.group.memo

// DTO (네가 올린 파일 기준)
// 그룹 정보 조회용 레포 (이미 프로젝트에 존재)
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.feature.controller.service.MyPageService
import com.ssafy.a705.group.common.GroupMemberManager
import com.ssafy.a705.group.common.model.Memo
import com.ssafy.a705.group.common.util.GroupStatusUtil
import com.ssafy.a705.group.list.GroupRepository
import com.ssafy.a705.common.network.GatheringUpdateRequest
import com.ssafy.a705.common.network.GroupApiService
import com.ssafy.a705.common.network.sign.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class GroupMemoUiState(
    val isLoading: Boolean = false,
    val groupName: String = "",
    val status: String = "",
    val gatheringTime: String? = null,
    val gatheringLocation: String? = null,
    val memos: List<Memo> = emptyList(),
    // 모임 정보 수정을 위한 임시 상태
    val tempMeetingTime: String? = null,
    val tempMeetingLocation: String? = null,
    val isSavingMeetingInfo: Boolean = false,
    // 스크롤 제어를 위한 상태
    val shouldScrollToTop: Boolean = false
)

@HiltViewModel
class GroupMemoViewModel @Inject constructor(
    private val groupApiService: GroupApiService,
    private val groupMemoRepository: GroupMemoRepository,
    private val groupRepository: GroupRepository,
    private val sessionManager: SessionManager,
    private val myPageService: MyPageService
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupMemoUiState())
    val uiState: StateFlow<GroupMemoUiState> = _uiState



    /**
     * 그룹 정보와 메모 목록을 로드
     */
    fun loadGroupInfoAndMemos(groupId: Long) {
        println("🔍 GroupMemoViewModel.loadGroupInfoAndMemos 호출 - groupId: $groupId")
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                // 그룹 기본 정보 로드
                val groupInfo = groupRepository.getGroupInfo(groupId)
                println("🔍 그룹 정보: status=${groupInfo.status}, startAt=${groupInfo.startAt}, endAt=${groupInfo.endAt}")
                
                val updatedStatus = GroupStatusUtil.getAutoUpdatedStatus(
                    groupInfo.status, 
                    groupInfo.startAt, 
                    groupInfo.endAt
                )
                println("🔍 자동 업데이트된 상태: $updatedStatus")
                
                val displayStatus = GroupStatusUtil.getDisplayStatus(updatedStatus)
                println("🔍 UI 표시용 상태: $displayStatus")
                
                // 모임 정보 로드 (별도 API)
                val gatheringInfo = try {
                    groupRepository.getGatheringInfo(groupId)
                } catch (e: Exception) {
                    println("⚠️ 모임 정보 조회 실패: ${e.message}")
                    null
                }
                
                _uiState.value = _uiState.value.copy(
                    groupName = groupInfo.name,
                    status = displayStatus,
                    gatheringTime = gatheringInfo?.gatheringTime?.let { formatServerDateTimeForDisplay(it) },
                    gatheringLocation = gatheringInfo?.gatheringLocation,
                    isLoading = false
                )
                
                // 메모 목록 로드
                val apiList = groupMemoRepository.getAllMemos(groupId).map { dto ->
                    // 현재 사용자의 닉네임과 메모 작성자를 비교하여 소유권 확인
                    val currentUserProfile = myPageService.getMyProfile()
                    val isCurrentUserMemo = currentUserProfile.nickname == dto.writer
                    
                    Memo(
                        id = dto.memoId.toInt(),
                        content = dto.content,
                        isEditing = false,
                        isMine = isCurrentUserMemo,
                        writer = dto.writer
                    )
                }
                println("🔍 메모 목록 조회 성공 - 개수: ${apiList.size}")
                
                _uiState.value = _uiState.value.copy(memos = apiList)
                
            } catch (e: Exception) {
                println("❌ 그룹 정보 또는 메모 로딩 실패: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * 날짜 시간 포맷팅 함수
     */

    // 화면 표기용: 서버 한국시간 ISO 형식 → "yyyy-MM-dd HH:mm" 또는 "HH:mm" (오늘 날짜면 시간만)
    private fun formatServerDateTimeForDisplay(input: String): String {
        return try {
            // 서버에서 받은 한국시간 ISO 형식 (밀리초 있거나 없거나)
            val localDateTime = runCatching {
                // 1) 밀리초 포함 형식 (예: 2025-08-17T22:00:00.000)
                java.time.LocalDateTime.parse(input, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
            }.getOrNull() ?: runCatching {
                // 2) 밀리초 없는 형식 (예: 2025-08-17T22:00:00)
                java.time.LocalDateTime.parse(input, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            }.getOrNull() ?: runCatching {
                // 3) 초 없는 형식 (예: 2025-08-17T22:00)
                java.time.LocalDateTime.parse(input, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
            }.getOrThrow()
            
            // 오늘 날짜인지 확인
            val today = java.time.LocalDate.now()
            val inputDate = localDateTime.toLocalDate()
            
            if (inputDate == today) {
                // 오늘 날짜면 시간만 표시
                localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            } else {
                // 다른 날짜면 날짜+시간 표시
                localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            }
        } catch (e: Exception) {
            println("⚠️ 날짜 포맷팅 실패: ${e.message} / raw='$input'")
            input // 실패해도 화면 망가지지 않도록 원본 리턴
        }
    }




    /**
     * 현재 사용자의 groupMemberId를 찾아서 GroupMemberManager에 설정
     */
    private suspend fun setCurrentUserGroupMemberId(groupId: Long) {
        try {
            // 현재 사용자의 닉네임 가져오기
            val currentUserProfile = myPageService.getMyProfile()
            
            // 그룹 멤버 목록에서 현재 사용자의 groupMemberId 찾기
            val membersResponse = groupApiService.getGroupMembers(groupId)
            val currentUserMember = membersResponse.data?.groupMembers?.find { member ->
                member.nickname == currentUserProfile.nickname
            }
            
            currentUserMember?.let { member ->
                GroupMemberManager.setGroupMemberId(member.groupMemberId.toInt())
                println("✅ 현재 사용자 groupMemberId 설정: ${member.groupMemberId}")
            }
        } catch (e: Exception) {
            println("⚠️ 현재 사용자 groupMemberId 설정 실패: ${e.message}")
        }
    }

    // 모임 정보 수정 관련 메서드들
    fun updateMeetingTime(time: String) {
        val currentState = _uiState.value
        val currentDate = currentState.tempMeetingTime?.split(" ")?.get(0) ?: ""
        val newDateTime = if (currentDate.isNotEmpty()) "$currentDate $time" else time
        
        _uiState.value = currentState.copy(tempMeetingTime = newDateTime)
    }
    
    fun updateMeetingDate(date: String) {
        val currentState = _uiState.value
        val currentTime = currentState.tempMeetingTime?.split(" ")?.getOrNull(1) ?: ""
        val newDateTime = if (currentTime.isNotEmpty()) "$date $currentTime" else date
        
        _uiState.value = currentState.copy(tempMeetingTime = newDateTime)
    }
    
    fun updateMeetingLocation(location: String) {
        _uiState.value = _uiState.value.copy(tempMeetingLocation = location)
    }
    
    fun resetMeetingInfo() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            tempMeetingTime = currentState.gatheringTime,
            tempMeetingLocation = currentState.gatheringLocation
        )
    }
    
    // 새 메모 생성 (플러스 버튼 클릭 시)
    fun createNewMemo() {
        val currentState = _uiState.value
        val newMemo = Memo(
            id = generateTempId(), // 임시 음수 ID
            content = "",
            isEditing = true,
            isMine = true,
            writer = "나"
        )
        
        _uiState.value = currentState.copy(
            memos = listOf(newMemo) + currentState.memos // 새 메모를 맨 위에 추가
        )
    }
    
    // 임시 ID 생성 (음수 값으로 실제 메모와 구분)
    private fun generateTempId(): Int {
        val existingTempIds = _uiState.value.memos
            .filter { it.id < 0 }
            .map { it.id }
        return if (existingTempIds.isEmpty()) -1 else existingTempIds.min() - 1
    }
    
    fun saveMeetingInfo(groupId: Long) {
        val state = _uiState.value
        if (state.tempMeetingTime.isNullOrBlank() || state.tempMeetingLocation.isNullOrBlank()) {
            return // 시간과 장소가 모두 입력되어야 함
        }
        
        _uiState.value = state.copy(isSavingMeetingInfo = true)
        
        viewModelScope.launch {
            try {
                // 시간 파싱 및 변환
                val localDateTime = runCatching {
                    // 1) 날짜+시간 형식 (예: 2025-08-17 22:00)
                    LocalDateTime.parse(
                        state.tempMeetingTime,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    )
                }.getOrNull() ?: runCatching {
                    // 2) 시간만 입력된 경우 (예: 22:00) → 1년 뒤 날짜로 설정
                    val time = LocalTime.parse(state.tempMeetingTime, DateTimeFormatter.ofPattern("HH:mm"))
                    LocalDateTime.of(LocalDate.now().plusYears(1), time)
                }.getOrThrow()
                
                val kstIso = convertToKstIso(localDateTime)
                
                val patchReq = GatheringUpdateRequest(
                    gatheringTime = kstIso,
                    gatheringLocation = state.tempMeetingLocation
                )
                
                groupRepository.updateGathering(groupId, patchReq)
                
                // 성공 시 실제 상태 업데이트
                _uiState.value = state.copy(
                    gatheringTime = state.tempMeetingTime,
                    gatheringLocation = state.tempMeetingLocation,
                    isSavingMeetingInfo = false
                )
                
                println("✅ 모임 정보 수정 성공")
                
            } catch (e: Exception) {
                println("❌ 모임 정보 수정 실패: ${e.message}")
                _uiState.value = state.copy(isSavingMeetingInfo = false)
                // TODO: 에러 처리 (스낵바 등)
            }
        }
    }
    
    // 시간 변환 유틸 함수 (이미 한국시간으로 가정)
    private fun convertToKstIso(localDateTime: LocalDateTime): String {
        // 이미 한국시간으로 가정하므로 추가 변환 없이 포맷만 변경
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
    }

    fun updateMemo(id: Int, content: String) {
        _uiState.value = _uiState.value.copy(
            memos = _uiState.value.memos.map {
                if (it.id == id) it.copy(content = content) else it
            }
        )
    }

    fun editMemo(id: Int) {
        _uiState.value = _uiState.value.copy(
            memos = _uiState.value.memos.map {
                if (it.id == id) it.copy(isEditing = true) else it
            }
        )
    }

    /**
     * 메모 저장 (새 메모 생성 또는 기존 메모 수정)
     * 새 메모: POST 요청 (memoId가 음수인 경우)
     * 기존 메모: PUT 요청 (memoId가 양수인 경우)
     */
    fun saveMemo(groupId: Long, memoId: Int, content: String) {
        if (content.trim().isBlank()) {
            // 내용이 비어있으면 저장하지 않음
            if (memoId < 0) {
                // 새 메모인 경우 삭제
                removeTempMemo(memoId)
            }
            return
        }
        
        viewModelScope.launch {
            try {
                if (memoId < 0) {
                    // 새 메모 생성 (POST)
                    println("📝 새 메모 생성: $content")
                    groupMemoRepository.createMemo(groupId, content.trim())
                    
                    // 임시 메모 제거
                    removeTempMemo(memoId)
                    
                    // 목록 새로고침 후 스크롤 플래그 설정 (새 메모만)
                    loadGroupInfoAndMemos(groupId)
                    _uiState.value = _uiState.value.copy(shouldScrollToTop = true)
                } else {
                    // 기존 메모 수정 (PUT)
                    println("📝 메모 수정: ID=$memoId, content=$content")
                    
                    // 현재 사용자의 groupMemberId를 찾아서 사용
                    val groupMemberId = findCurrentUserGroupMemberIdForUpdate(groupId)
                    
                    groupMemoRepository.updateMemo(
                        groupId = groupId,
                        memoId = memoId.toLong(),
                        request = MemoUpdateRequestDto(groupMemberId, content.trim())
                    )
                    
                    // 편집 모드 해제
                    _uiState.value = _uiState.value.copy(
                        memos = _uiState.value.memos.map {
                            if (it.id == memoId) it.copy(isEditing = false) else it
                        }
                    )
                    
                    // 목록 새로고침 (수정은 스크롤하지 않음)
                    loadGroupInfoAndMemos(groupId)
                }
            } catch (e: Exception) {
                println("⚠️ 메모 저장 실패: ${e.message}")
                // TODO: 에러 처리 (스낵바 등)
            }
        }
    }
    
    /**
     * 메모 수정을 위한 현재 사용자의 groupMemberId를 찾는 함수
     */
    private suspend fun findCurrentUserGroupMemberIdForUpdate(groupId: Long): Long {
        return try {
            // 현재 사용자의 닉네임 가져오기
            val currentUserProfile = myPageService.getMyProfile()
            
            // 그룹 멤버 목록에서 현재 사용자의 groupMemberId 찾기
            val membersResponse = groupApiService.getGroupMembers(groupId)
            val currentUserMember = membersResponse.data?.groupMembers?.find { member ->
                member.nickname == currentUserProfile.nickname
            }
            
            currentUserMember?.groupMemberId ?: -1L
        } catch (e: Exception) {
            println("⚠️ 메모 수정용 groupMemberId 찾기 실패: ${e.message}")
            -1L
        }
    }
    
    // 임시 메모 제거
    private fun removeTempMemo(tempId: Int) {
        _uiState.value = _uiState.value.copy(
            memos = _uiState.value.memos.filter { it.id != tempId }
        )
    }

    fun deleteMemo(groupId: Long, memoId: Int) {
        viewModelScope.launch {
            try {
                groupMemoRepository.deleteMemo(groupId, memoId.toLong())
                // 목록 새로고침
                loadGroupInfoAndMemos(groupId)
            } catch (e: Exception) {
                println("⚠️ 메모 삭제 실패: ${e.message}")
                // TODO: 에러 처리 (스낵바 등)
            }
        }
    }
    
    /**
     * 스크롤 플래그 리셋
     */
    fun resetScrollFlag() {
        _uiState.value = _uiState.value.copy(shouldScrollToTop = false)
    }
} 
