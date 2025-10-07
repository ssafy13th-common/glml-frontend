package com.ssafy.a705.feature.record.diary.ui.state

import android.net.Uri

data class RecordUpdateUiState(
    val diaryId: Long? = null,

    val locationCode: Int? = null,
    val locationName: String? = null,
    val startedAtDisplay: String = "", // yyyy.MM.dd
    val endedAtDisplay: String = "",
    val content: String = "",

    val keepServerKeys: List<String> = mutableListOf(),
    val newPhotoUris: List<Uri> = emptyList(),

    val isSaving: Boolean = false,
    val error: String? = null
)
