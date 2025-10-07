package com.ssafy.a705.feature.record.diary.data.model.request

data class RecordUpdateRequest(
    val locationCode: Int,
    val startedAt: String,
    val endedAt: String,
    val content: String?,
    val keepImageUrls: List<String>?,
    val newImageUrls: List<String>?
)
