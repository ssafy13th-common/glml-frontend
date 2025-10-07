package com.ssafy.a705.feature.record.diary.data.repository

import android.util.Log
import com.ssafy.a705.feature.record.diary.data.paging.RecordPagingSource
import com.ssafy.a705.feature.record.diary.domain.repository.RecordRepository
import com.ssafy.a705.feature.record.diary.data.model.RecordDetailItem
import com.ssafy.a705.feature.record.diary.data.model.request.RecordCreateRequest
import com.ssafy.a705.feature.record.diary.data.model.request.RecordUpdateRequest
import com.ssafy.a705.feature.record.diary.data.source.RecordRemoteDataSource
import javax.inject.Inject

class RecordRepositoryImpl @Inject constructor(
    private val remote: RecordRemoteDataSource
) : RecordRepository {
    override suspend fun createRecord(req: RecordCreateRequest): Result<Long> = runCatching {
        val resp = remote.createDiary(req)
        val id = resp.data?.id
            ?: throw IllegalStateException(resp.message ?: "생성 실패: ID 누락")
        Log.d("RecordRepository", "resp: ${resp.message}")
        id
    }

    override fun diaryPagingSource(
        pageSize: Int,
        locationCode: Int?
    ) = RecordPagingSource(remote, pageSize, locationCode)

    override suspend fun getDiaryDetail(id: Long): Result<RecordDetailItem> = runCatching {
        val resp = remote.getDiaryDetail(id)
        val err = resp.message
        val body = resp.data
        if (err != null || body == null) {
            throw IllegalStateException(err ?: "상세 데이터를 가져오지 못했습니다.")
        }
        body
    }

    override suspend fun updateRecord(
        id: Long,
        req: RecordUpdateRequest
    ): Result<Unit> = runCatching {
        val resp = remote.updateDiary(id, req)
        if (resp.message != null) throw IllegalStateException(resp.message)
    }

    override suspend fun deleteRecord(id: Long): Result<Unit> = runCatching {
        val resp = remote.deleteDiary(id)
        val err = resp.message
        if (err != null) throw IllegalStateException(err)
    }
}