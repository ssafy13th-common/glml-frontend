// GroupMemberViewModel.kt (교체)
package com.ssafy.a705.feature.group.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.common.network.GroupApiService
import com.ssafy.a705.feature.group.common.GroupMemberManager
import com.ssafy.a705.feature.group.common.util.GroupStatusUtil
import com.ssafy.a705.common.network.sign.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
// GroupMemberViewModel.kt (핵심만)
@HiltViewModel
class GroupMemberViewModel @Inject constructor(
    private val groupApi: GroupApiService,
    private val groupMemberRepository: GroupMemberRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName

    private val _groupStatus = MutableStateFlow("")
    val groupStatus: StateFlow<String> = _groupStatus

    private val _members = MutableStateFlow<List<MemberItem>>(emptyList())
    val members: StateFlow<List<MemberItem>> = _members

    // 검색 상태(그대로 유지)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    private val _searchResults = MutableStateFlow<List<MemberItem>>(emptyList())
    val searchResults: StateFlow<List<MemberItem>> = _searchResults

    // 현재 사용자의 이메일 정보
    private val currentUserEmail: String?
        get() = sessionManager.load()?.email

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .mapLatest { q ->
                    if (q.isBlank()) emptyList()
                    else groupMemberRepository.search(q).map { 
                        MemberItem(
                            id = it.email, 
                            name = it.nickname,
                            email = it.email,
                            profileImageUrl = it.profileUrl,
                            role = "MEMBER"  // 검색된 사용자는 기본적으로 MEMBER 역할
                        ) 
                    }
                }
                .collect { _searchResults.value = it }
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    /** 진입/새로고침: 그룹 헤더 + 멤버 목록 서버에서 조회 */
    fun loadGroup(groupId: Long) {
        println("🔍 GroupMemberViewModel.loadGroup 호출 - groupId: $groupId")
        viewModelScope.launch {
            try {
                groupApi.getGroupInfo(groupId).data?.let { info ->
                    _groupName.value = info.name
                    
                    // 자동 상태 변경 적용
                    val updatedStatus = GroupStatusUtil.getAutoUpdatedStatus(
                        info.status, 
                        info.startAt, 
                        info.endAt
                    )
                    
                    _groupStatus.value = GroupStatusUtil.getDisplayStatus(updatedStatus)
                }
                val membersList = groupMemberRepository.getMembers(groupId)
                println("🔍 ViewModel에서 받은 멤버 목록: $membersList")
                _members.value = membersList
                
                // 현재 사용자의 groupMemberId를 찾아서 GroupMemberManager에 설정
                setCurrentUserGroupMemberId(groupId, membersList)
            } catch (e: Exception) {
                println("❌ 멤버 목록 로딩 실패: ${e.message}")
                _members.value = emptyList()
            }
        }
    }

    /**
     * 현재 사용자의 groupMemberId를 찾아서 GroupMemberManager에 설정
     */
    private suspend fun setCurrentUserGroupMemberId(groupId: Long, membersList: List<MemberItem>) {
        println("🔍 setCurrentUserGroupMemberId 시작 - 현재 사용자 이메일: $currentUserEmail")
        println("🔍 멤버 목록: ${membersList.map { "${it.name}(${it.email})" }}")
        
        if (currentUserEmail != null) {
            try {
                // 1. 이메일 기반 매칭을 우선적으로 시도
                val currentUserMember = membersList.find { member ->
                    member.email == currentUserEmail
                }
                
                if (currentUserMember != null) {
                    GroupMemberManager.setGroupMemberId(currentUserMember.id.toIntOrNull() ?: -1)
                    println("✅ 현재 사용자 groupMemberId 설정 (이메일 매칭): ${currentUserMember.id}")
                    return
                }
                
                // 2. 이메일 매칭이 실패한 경우, 회원 검색 API를 사용하여 현재 사용자 정보 가져오기
                println("🔍 이메일 매칭 실패, 회원 검색 API 시도")
                val searchResponse = groupApi.searchMembers(currentUserEmail!!)
                val currentUserInfo = searchResponse.data?.members?.find { it.email == currentUserEmail }
                
                if (currentUserInfo != null) {
                    println("🔍 회원 검색 API 결과: ${currentUserInfo.nickname}")
                    // 그룹 멤버 목록에서 현재 사용자의 groupMemberId 찾기
                    val currentUserMember = membersList.find { member ->
                        member.name == currentUserInfo.nickname
                    }
                    
                    currentUserMember?.let { member ->
                        GroupMemberManager.setGroupMemberId(member.id.toIntOrNull() ?: -1)
                        println("✅ 현재 사용자 groupMemberId 설정 (닉네임 매칭): ${member.id}")
                    } ?: run {
                        println("⚠️ 닉네임 매칭도 실패: ${currentUserInfo.nickname}")
                    }
                } else {
                    println("⚠️ 회원 검색 API에서 현재 사용자 정보를 찾지 못함")
                    // 3. 검색 결과가 없으면 이메일 앞부분으로 매칭 시도
                    val emailPrefix = currentUserEmail?.split("@")?.firstOrNull()
                    println("🔍 이메일 앞부분 매칭 시도: $emailPrefix")
                    val currentUserMember = membersList.find { member ->
                        member.name == emailPrefix
                    }
                    
                    currentUserMember?.let { member ->
                        GroupMemberManager.setGroupMemberId(member.id.toIntOrNull() ?: -1)
                        println("✅ 현재 사용자 groupMemberId 설정 (이메일 앞부분 매칭): ${member.id}")
                    } ?: run {
                        println("⚠️ 이메일 앞부분 매칭도 실패: $emailPrefix")
                    }
                }
            } catch (e: Exception) {
                println("❌ 현재 사용자 groupMemberId 설정 실패: ${e.message}")
            }
        } else {
            println("⚠️ 현재 사용자 이메일 정보가 없음")
        }
    }

    /** 검색 결과에서 선택 → 이메일 초대 → 성공 후 재조회 */
    fun inviteMember(groupId: Long, member: MemberItem) {
        viewModelScope.launch {
            try { groupMemberRepository.inviteByEmail(groupId, member.id) } catch (_: Exception) { }
            finally {
                loadGroup(groupId)
                _searchQuery.value = ""
                _searchResults.value = emptyList()
            }
        }
    }
}
