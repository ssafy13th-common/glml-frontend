package com.ssafy.a705.feature.group.create

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.group.common.util.GeoUtil
import com.ssafy.a705.common.network.GroupApiService
import com.ssafy.a705.common.network.GroupCreateRequest
import com.ssafy.a705.common.network.GroupUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

@HiltViewModel
class GroupCreateViewModel @Inject constructor(
    private val api: GroupApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupCreateUiState())
    val uiState: StateFlow<GroupCreateUiState> = _uiState

    /* ---------- UI 업데이트 ---------- */
    fun updateGroupName(name: String) {
        _uiState.value = _uiState.value.copy(
            groupName = name,
            isButtonEnabled = name.isNotBlank()
        )
    }
    fun updateDescription(desc: String) {
        _uiState.value = _uiState.value.copy(description = desc)
    }
    fun updateMeetingTime(time: String) {
        _uiState.value = _uiState.value.copy(meetingTime = time)
    }
    fun updateMeetingLocation(location: String) {
        _uiState.value = _uiState.value.copy(meetingLocation = location)
    }

    /* ---------- 그룹 생성 ---------- */
    fun createGroup(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successGroupId = null)

            try {
                val state = _uiState.value

                // 1) 그룹 생성: POST
                val createResp = api.createGroup(
                    GroupCreateRequest(
                        name = state.groupName,
                        summary = state.description.ifBlank { null }
                    )
                )
                if (createResp.message != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = createResp.message
                    )
                    return@launch
                }

                val newGroupId: Long = createResp.data?.groupId ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "생성된 그룹 ID를 확인할 수 없습니다."
                    )
                    return@launch
                }

                // 2) 그룹 정보 업데이트: PUT (모임 시간/장소 포함)
                val kstIso = buildKstMeetingTime(state)  // ← 핵심: 유연 파싱 + KST 변환

                // 지오코딩 수행 (장소가 입력된 경우에만)
                val coordinates = if (state.meetingLocation.isNotBlank()) {
                    GeoUtil.geocodeKoreaOrNull(context, state.meetingLocation)
                } else null

                val updateResp = api.updateGroupInfo(
                    groupId = newGroupId,
                    request = GroupUpdateRequest(
                        name = state.groupName,
                        summary = state.description.ifBlank { null },
                        gatheringTime = kstIso,
                        gatheringLocation = state.meetingLocation.ifBlank { null },
                        locationLatitude = coordinates?.first,
                        locationLongitude = coordinates?.second,
                        startAt = null,
                        endAt = null,
                        feePerMinute = null
                    )
                )
                if (updateResp.message != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = updateResp.message
                    )
                    return@launch
                }

                // 3) 성공
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successGroupId = newGroupId
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "네트워크 오류가 발생했습니다."
                )
            }
        }
    }

    /* ---------- 유틸: 시간 파싱 & 변환 ---------- */
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
        // 마지막: 기본 파서(예외 던짐) — 되도록 위 패턴에서 걸리도록 유지
        return LocalDateTime.parse(input.trim())
    }

    // "02:30 PM" / "오후 2:30" / "14:30" → LocalTime
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
        // 마지막 폴백(실패 시 예외)
        return LocalTime.parse(input.trim())
    }

    // 로컬(LocalDateTime) → 한국시간 ISO 형식 (이미 한국시간으로 가정)
    private fun convertToKstIso(localDateTime: LocalDateTime): String {
        // 이미 한국시간으로 가정하므로 추가 변환 없이 포맷만 변경
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
    }

    // UI의 meetingTime 문자열을 해석해 KST ISO로 만든다.
    // 1) 날짜+시간이 함께 오면 그대로 파싱 → KST 변환
    // 2) 시간만 오면 기준 날짜는 "1년 뒤(LocalDate.now().plusYears(1))"을 사용 → KST 변환
    private fun buildKstMeetingTime(state: GroupCreateUiState): String? {
        val raw = state.meetingTime.trim()
        if (raw.isBlank()) return null

        // 1) 먼저 LocalDateTime으로 직접 파싱 시도 (날짜가 함께 온 경우)
        runCatching { return convertToKstIso(parseLocalDateTimeFlexible(raw)) }

        // 2) 시간만 온 경우(LocalTime) → 기준 날짜는 1년 뒤
        val time = runCatching { parseLocalTimeFlexible(raw) }.getOrNull() ?: return null
        val baseDate = LocalDate.now().plusYears(1) // 1년 뒤 날짜

        val ldt = LocalDateTime.of(baseDate, time)
        return convertToKstIso(ldt)
    }
}
