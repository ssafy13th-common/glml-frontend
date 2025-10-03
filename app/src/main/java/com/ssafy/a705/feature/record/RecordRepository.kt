package com.ssafy.a705.feature.record

import android.util.Log
import javax.inject.Inject

class RecordRepository @Inject constructor(
    private val api: RecordApi
) {
    suspend fun createRecord(req: RecordCreateRequest): Result<Long> = runCatching {
        val resp = api.createDiary(req)
        val id = resp.data?.id
            ?: throw IllegalStateException(resp.message ?: "생성 실패: ID 누락")
        Log.d("RecordRepository", "resp: ${resp.message}")
        id
    }

    fun diaryPagingSource(
        pageSize: Int,
        locationCode: Int?
    ) = RecordPagingSource(api, pageSize, locationCode)

    suspend fun getDiaryDetail(id: Long): Result<RecordDetailItem> = runCatching {
        val resp = api.getDiaryDetail(id)
        val err = resp.message
        val body = resp.data
        if (err != null || body == null) {
            throw IllegalStateException(err ?: "상세 데이터를 가져오지 못했습니다.")
        }
        body
    }

    suspend fun updateRecord(
        id: Long,
        req: RecordUpdateRequest
    ): Result<Unit> = runCatching {
        val resp = api.updateDiary(id, req)
        if (resp.message != null) throw IllegalStateException(resp.message)
    }

    suspend fun deleteRecord(id: Long): Result<Unit> = runCatching {
        val resp = api.deleteDiary(id)
        val err = resp.message
        if (err != null) throw IllegalStateException(err)
    }
}