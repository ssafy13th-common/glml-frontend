package com.ssafy.a705.domain.group.edit

data class GroupEditUiState(
    val groupName: String = "",
    val description: String = "",
    val status: String = "TO_DO",
    val startAt: String = "",
    val endAt: String = "",
    val feePerMinute: String = "",
    val gatheringTime: String = "",
    val gatheringLocation: String = "",
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val isButtonEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)