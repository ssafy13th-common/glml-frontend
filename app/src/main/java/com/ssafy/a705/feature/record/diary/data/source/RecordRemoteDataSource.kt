package com.ssafy.a705.feature.record.diary.data.source

import com.ssafy.a705.common.model.BasicResponse
import com.ssafy.a705.feature.record.diary.data.model.DiaryDetailResponse
import com.ssafy.a705.feature.record.diary.data.model.DiaryListResponse
import com.ssafy.a705.feature.record.diary.data.model.request.RecordCreateRequest
import com.ssafy.a705.feature.record.diary.data.model.request.RecordCreateResponse
import com.ssafy.a705.feature.record.diary.data.model.request.RecordUpdateRequest
import javax.inject.Inject

class RecordRemoteDataSource @Inject constructor(
    private val api: RecordApi
) {
    suspend fun createDiary(body: RecordCreateRequest): RecordCreateResponse =
        api.createDiary(body)

    suspend fun getDiaries(
        cursorId: Long?,
        size: Int,
        locationCode: Int?
    ): DiaryListResponse = api.getDiaries(cursorId, size, locationCode)

    suspend fun getDiaryDetail(id: Long): DiaryDetailResponse =
        api.getDiaryDetail(id)

    suspend fun updateDiary(id: Long, body: RecordUpdateRequest): BasicResponse =
        api.updateDiary(id, body)

    suspend fun deleteDiary(id: Long): BasicResponse =
        api.deleteDiary(id)
}