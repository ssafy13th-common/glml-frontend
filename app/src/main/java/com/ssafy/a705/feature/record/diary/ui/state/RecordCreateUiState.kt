package com.ssafy.a705.feature.record.diary.ui.state

import android.net.Uri

data class RecordCreateUiState(
    val code: String = "",
    val location: String = "",
    val startDate: String? = null,
    val endDate: String? = null,
    val photoUris: List<Uri> = emptyList(),
    val diary: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)
