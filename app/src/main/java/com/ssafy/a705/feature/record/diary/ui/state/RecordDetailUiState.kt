package com.ssafy.a705.feature.record.diary.ui.state

import com.ssafy.a705.feature.record.diary.data.model.RecordDetailItem

data class RecordDetailUiState(
    val loading: Boolean = false,
    val data: RecordDetailItem? = null,
    val error: String? = null
)
