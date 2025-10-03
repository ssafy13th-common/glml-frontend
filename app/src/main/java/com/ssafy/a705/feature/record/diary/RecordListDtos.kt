package com.ssafy.a705.feature.record.diary

data class RecordListItem(
    val id: Long,
    val location: String,
    val startedAt: String,
    val endedAt: String,
    val summary: String?,
    val thumbnailUrl: String?
)

data class DiaryListData(
    val diaries: List<RecordListItem>
)

data class DiaryListResponse(
    val message: String?,
    val data: DiaryListData?
)

data class RecordDetailItem(
    val location: String,
    val startedAt: String,
    val endedAt: String,
    val content: String?,
    val imageUrls: List<String>?
)

data class DiaryDetailResponse(
    val message: String?,
    val data: RecordDetailItem?
)