package com.ssafy.a705.feature.record.diary.data.model.request

data class RecordCreateRequest(
    val locationCode: Int,
    val startedAt: String,   // yyyy-MM-dd
    val endedAt: String,     // yyyy-MM-dd
    val content: String?,
    val imageUrls: List<String>?
)

data class RecordCreateResponse(
    val message: String?,
    val data: CreateData?
) {
    data class CreateData(val id: Long)
}