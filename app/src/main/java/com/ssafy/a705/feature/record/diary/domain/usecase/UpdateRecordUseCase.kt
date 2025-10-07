package com.ssafy.a705.feature.record.diary.domain.usecase

import com.ssafy.a705.feature.record.diary.data.model.request.RecordUpdateRequest
import com.ssafy.a705.feature.record.diary.domain.repository.RecordRepository
import javax.inject.Inject

class UpdateRecordUseCase @Inject constructor(
    private val repo: RecordRepository
) {
    suspend operator fun invoke(id: Long, req: RecordUpdateRequest): Result<Unit> =
        repo.updateRecord(id, req)
}