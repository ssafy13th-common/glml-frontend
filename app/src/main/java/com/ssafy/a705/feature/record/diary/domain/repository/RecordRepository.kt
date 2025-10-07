package com.ssafy.a705.feature.record.diary.domain.repository

import androidx.paging.PagingSource
import com.ssafy.a705.feature.record.diary.data.model.RecordDetailItem
import com.ssafy.a705.feature.record.diary.data.model.RecordListItem
import com.ssafy.a705.feature.record.diary.data.model.request.RecordCreateRequest
import com.ssafy.a705.feature.record.diary.data.model.request.RecordUpdateRequest

interface RecordRepository {
    suspend fun createRecord(req: RecordCreateRequest): Result<Long>

    fun diaryPagingSource(
        pageSize: Int,
        locationCode: Int?
    ) : PagingSource<Long, RecordListItem>

    suspend fun getDiaryDetail(id: Long): Result<RecordDetailItem>

    suspend fun updateRecord(
        id: Long,
        req: RecordUpdateRequest
    ): Result<Unit>

    suspend fun deleteRecord(id: Long): Result<Unit>
}