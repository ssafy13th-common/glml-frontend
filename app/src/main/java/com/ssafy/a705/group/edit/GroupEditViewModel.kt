package com.ssafy.a705.group.edit

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.group.common.util.GeoUtil
import com.ssafy.a705.group.common.util.GroupStatusUtil
import com.ssafy.a705.global.network.GroupApiService
import com.ssafy.a705.global.network.GroupUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GroupEditViewModel @Inject constructor(
    private val api: GroupApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupEditUiState())
    val uiState: StateFlow<GroupEditUiState> = _uiState

    private var currentGroupId: Long? = null

    // -----------------------
    // UI 업데이트
    // -----------------------
    fun updateGroupName(name: String) {
        _uiState.value = _uiState.value.copy(
            groupName = name,
            isButtonEnabled = name.isNotBlank()
        )
    }
    fun updateDescription(desc: String) { _uiState.value = _uiState.value.copy(description = desc) }
    fun updateStartDate(date: String) { _uiState.value = _uiState.value.copy(startAt = date) }
    fun updateEndDate(date: String) { _uiState.value = _uiState.value.copy(endAt = date) }
    fun updateFeePerMinute(fee: String) { _uiState.value = _uiState.value.copy(feePerMinute = fee) }
    fun updateGatheringTime(time: String) { _uiState.value = _uiState.value.copy(gatheringTime = time) }
    fun updateGatheringLocation(location: String) { _uiState.value = _uiState.value.copy(gatheringLocation = location) }

    // -----------------------
    // 초기 로드
    // -----------------------
    fun initGroupInfo(groupId: Long) {
        currentGroupId = groupId
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val gid = currentGroupId ?: return@launch

                // 그룹 기본 정보
                val groupResp = api.getGroupInfo(gid)
                val groupData = groupResp.data

                // 모임 정보(없을 수 있음)
                val gatheringResp = runCatching { api.getGatheringInfo(gid) }.getOrNull()
                val gatheringData = gatheringResp?.data

                if (groupData != null) {
                    val updatedStatus = GroupStatusUtil.getAutoUpdatedStatus(
                        groupData.status,
                        groupData.startAt,
                        groupData.endAt
                    )

                    val localGatheringTime = gatheringData?.gatheringTime?.let { formatDateTime(it) } ?: ""

                    // 서버가 yyyy-MM-dd 또는 Instant/OffsetDateTime/로컬DATETIME 다 올 수 있으니 대응
                    val displayStartAt = groupData.startAt?.let { formatDateForDisplay(it) } ?: ""
                    val displayEndAt = groupData.endAt?.let { formatDateForDisplay(it) } ?: ""

                    _uiState.value = _uiState.value.copy(
                        groupName = groupData.name,
                        description = groupData.summary ?: "",
                        status = updatedStatus,
                        startAt = displayStartAt,
                        endAt = displayEndAt,
                        feePerMinute = groupData.feePerMinute?.toString() ?: "",
                        gatheringTime = localGatheringTime,
                        gatheringLocation = gatheringData?.gatheringLocation ?: "",
                        isButtonEnabled = true,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = groupResp.message ?: "그룹 정보를 불러올 수 없습니다."
                    )
                }
            } catch (e: Exception) {
                Log.e("GroupEdit", "initGroupInfo() 실패: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "네트워크 오류가 발생했습니다."
                )
            }
        }
    }

    // -----------------------
    // 수정 저장
    // -----------------------
    fun updateGroup(context: Context) {
        val gid = currentGroupId ?: return
        val state = _uiState.value

        // 종료일 유효성
        val startDateOrNull = state.startAt.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
        }
        val endDateOrNull = state.endAt.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
        }
        if (startDateOrNull != null && endDateOrNull != null && endDateOrNull.isBefore(startDateOrNull)) {
            _uiState.value = state.copy(errorMessage = "종료일이 시작일보다 빠릅니다.", success = false)
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null, success = false)

        viewModelScope.launch {
            try {
                // 모임 시간 → KST ISO(예: 2025-08-12T11:00:34.902)로 변환
                val kstGatheringTime = buildKstGatheringTime(state)

                // 날짜는 yyyy-MM-dd 그대로 전송
                val dateOnlyOrNull: (String) -> String? = { s ->
                    if (s.isBlank()) null
                    else runCatching {
                        LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }.getOrNull()
                }

                // 지오코딩 수행 (장소가 입력된 경우에만)
                val coordinates = if (state.gatheringLocation.isNotBlank()) {
                    Log.d("GroupEditViewModel", "지오코딩 시작: ${state.gatheringLocation}")
                    val result = GeoUtil.geocodeKoreaOrNull(context, state.gatheringLocation)
                    if (result != null) {
                        Log.d("GroupEditViewModel", "지오코딩 성공: ${state.gatheringLocation} → (${result.first}, ${result.second})")
                    } else {
                        Log.e("GroupEditViewModel", "지오코딩 실패: ${state.gatheringLocation}")
                    }
                    result
                } else {
                    Log.d("GroupEditViewModel", "지오코딩 건너뜀: 장소명이 비어있음")
                    null
                }

                val putReq = GroupUpdateRequest(
                    name = state.groupName.trim(),
                    summary = state.description.ifBlank { null },
                    gatheringTime = kstGatheringTime,
                    gatheringLocation = state.gatheringLocation.ifBlank { null },
                    locationLatitude = coordinates?.first,
                    locationLongitude = coordinates?.second,
                    startAt = dateOnlyOrNull(state.startAt),  // yyyy-MM-dd
                    endAt = dateOnlyOrNull(state.endAt),      // yyyy-MM-dd
                    feePerMinute = state.feePerMinute.toIntOrNull()
                )
                Log.d("GroupEditViewModel", "그룹 수정 요청: locationLatitude=${coordinates?.first}, locationLongitude=${coordinates?.second}")
                api.updateGroupInfo(gid, putReq)

                // 저장 후 동기화
                val groupData = api.getGroupInfo(gid).data
                val finalStatus = if (groupData != null) {
                    GroupStatusUtil.getAutoUpdatedStatus(
                        groupData.status,
                        groupData.startAt,
                        groupData.endAt
                    )
                } else state.status

                val finalStartAt = groupData?.startAt?.let { formatDateForDisplay(it) } ?: state.startAt
                val finalEndAt = groupData?.endAt?.let { formatDateForDisplay(it) } ?: state.endAt

                _uiState.value = state.copy(
                    isLoading = false,
                    success = true,
                    groupName = groupData?.name ?: state.groupName,
                    description = groupData?.summary ?: state.description,
                    status = finalStatus,
                    startAt = finalStartAt,
                    endAt = finalEndAt,
                    feePerMinute = groupData?.feePerMinute?.toString() ?: state.feePerMinute
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "수정 중 오류가 발생했습니다."
                )
            }
        }
    }

    // -----------------------
    // 유틸
    // -----------------------

    // =======================
    // 유연한 날짜/시간 파서들
    // =======================
    // 서버 한국시간 ISO 형식 → "HH:mm" (수정 화면에서는 항상 시간만 표시)
    private fun formatDateTime(serverTime: String): String {
        return try {
            // 서버에서 받은 한국시간 ISO 형식 (밀리초 있거나 없거나)
            val localDateTime = runCatching {
                // 1) 밀리초 포함 형식 (예: 2025-08-17T22:00:00.000)
                LocalDateTime.parse(serverTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
            }.getOrNull() ?: runCatching {
                // 2) 밀리초 없는 형식 (예: 2025-08-17T22:00:00)
                LocalDateTime.parse(serverTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            }.getOrNull() ?: runCatching {
                // 3) 초 없는 형식 (예: 2025-08-17T22:00)
                LocalDateTime.parse(serverTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
            }.getOrThrow()
            
            // 수정 화면에서는 항상 시간만 표시
            localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            Log.e("GroupEdit", "시간 파싱 실패: ${e.message}")
            ""
        }
    }

    // 서버가 yyyy-MM-dd → 화면엔 "yyyy-MM-dd"
    private fun formatDateForDisplay(serverDate: String): String {
        return try {
            val ld = LocalDate.parse(serverDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            ld.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            Log.e("GroupEdit", "날짜 파싱 실패: ${e.message}")
            ""
        }
    }

    // 문자열 → LocalDateTime (여러 포맷 시도)
    private fun parseLocalDateTimeFlexible(input: String): LocalDateTime {
        val patterns = listOf(
            // 날짜+24시간
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            // 날짜+AM/PM
            "yyyy-MM-dd h:mm a",
            "yyyy-MM-dd h:mm:ss a"
        )
        for (p in patterns) {
            runCatching { return LocalDateTime.parse(input.trim(), DateTimeFormatter.ofPattern(p, Locale.US)) }
        }
        return LocalDateTime.parse(input.trim())
    }

    // "02:30 PM" / "오후 2:30" / "14:30" 등 → LocalTime
    private fun parseLocalTimeFlexible(input: String): LocalTime {
        val formatters = listOf(
            // 한글 AM/PM 형식들 (우선순위 높음)
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("a h:mm").toFormatter(Locale.KOREAN),
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("a h시").toFormatter(Locale.KOREAN),
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("ah:mm").toFormatter(Locale.KOREAN),
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("ah시").toFormatter(Locale.KOREAN),
            // 영문 AM/PM 형식들
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mm a").toFormatter(Locale.US),
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h a").toFormatter(Locale.US),
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("a h:mm").toFormatter(Locale.US),
            // 24시간 형식들
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("H:mm:ss")
        )
        for (f in formatters) {
            runCatching { return LocalTime.parse(input.trim(), f) }
        }
        // 마지막 폴백
        return LocalTime.parse(input.trim())
    }

    // 로컬 → 한국시간 ISO 형식 (이미 한국시간으로 가정)
    private fun convertToKstIso(localDateTime: LocalDateTime): String {
        // 이미 한국시간으로 가정하므로 추가 변환 없이 포맷만 변경
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
    }

    // UI에서 온 gatheringTime 문자열을 해석해서 KST ISO로 만들어줌
    private fun buildKstGatheringTime(state: GroupEditUiState): String? {
        val raw = state.gatheringTime.trim()
        if (raw.isBlank()) return null

        // 1) 먼저 LocalDateTime으로 직접 파싱 시도 (날짜가 함께 온 경우)
        runCatching { return convertToKstIso(parseLocalDateTimeFlexible(raw)) }

        // 2) 시간만 온 경우(LocalTime) → 기준 날짜는 startAt 또는 1년 뒤
        val time = runCatching { parseLocalTimeFlexible(raw) }.getOrNull() ?: return null
        val baseDate = state.startAt.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
        } ?: LocalDate.now().plusYears(1) // 1년 뒤 날짜

        val ldt = LocalDateTime.of(baseDate, time)
        return convertToKstIso(ldt)
    }
}
