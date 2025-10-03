package com.ssafy.a705.feature.group.create

// 그룹 생성 화면의 UI 상태 데이터
data class GroupCreateUiState(
    val groupName: String = "",
    val description: String = "",
    val meetingTime: String = "",          // DatePicker+TimePicker 결합 "yyyy-MM-dd HH:mm" 등
    val meetingLocation: String = "",
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val isLoading: Boolean = false,
    val isButtonEnabled: Boolean = false,
    val errorMessage: String? = null,
    val successGroupId: Long? = null       // ← Long으로 통일
)


// 실제 그룹 생성 API에 전달할 데이터 (나중에 Retrofit Request Body로 활용)
data class GroupCreateItem(
    val name: String,
    val summary: String?,
    val meetingTime: String?,
    val meetingLocation: String?,
    val members: List<String> = emptyList()
)