package com.ssafy.a705.feature.group.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.group.common.model.Group
import com.ssafy.a705.group.common.util.GroupStatusUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GroupListUiState(
    val planningGroups: List<Group> = emptyList(),
    val ongoingGroup: Group? = null,
    val finishedGroups: List<Group> = emptyList(),
    val isPlanningSelected: Boolean = true
)

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState

    fun toggleTab(isPlanning: Boolean) {
        _uiState.value = _uiState.value.copy(isPlanningSelected = isPlanning)
    }

    fun loadGroups() {
        viewModelScope.launch {
            try {
                val groups = groupRepository.getGroups()
                
                println("🔍 ViewModel received groups: ${groups.map { "id=${it.id}, name=${it.name}, status=${it.status}" }}")
                
                // 각 그룹의 상태를 자동 업데이트
                val updatedGroups = groups.map { group ->
                    // 그룹 상세 정보를 가져와서 시작일/종료일 확인
                    try {
                        val groupInfo = groupRepository.getGroupInfo(group.id)
                        val updatedStatus = GroupStatusUtil.getAutoUpdatedStatus(
                            group.status,
                            groupInfo.startAt,
                            groupInfo.endAt
                        )
                        group.copy(status = updatedStatus)
                    } catch (e: Exception) {
                        // 상세 정보 조회 실패시 원본 상태 유지
                        group
                    }
                }
                
                val planningGroups = updatedGroups.filter { it.status == GroupStatusUtil.STATUS_TO_DO }
                val ongoingGroup = updatedGroups.firstOrNull { it.status == GroupStatusUtil.STATUS_IN_PROGRESS }
                val finishedGroups = updatedGroups.filter { it.status == GroupStatusUtil.STATUS_DONE }
                
                println("🔍 ViewModel filtered: planning=${planningGroups.map { it.id }}, ongoing=${ongoingGroup?.id}, finished=${finishedGroups.map { it.id }}")
                
                _uiState.value = GroupListUiState(
                    planningGroups = planningGroups,
                    ongoingGroup = ongoingGroup,
                    finishedGroups = finishedGroups,
                    isPlanningSelected = true
                )
                
            } catch (e: Exception) {
                // 에러 처리 (필요시 사용자에게 알림)
            }
        }
    }
}
