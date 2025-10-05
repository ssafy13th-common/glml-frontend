package com.ssafy.a705.feature.record.diary.domain.usecase

import com.ssafy.a705.feature.record.diary.data.model.request.RecordCreateRequest
import com.ssafy.a705.feature.record.diary.domain.repository.RecordRepository
import javax.inject.Inject

class CreateRecordUseCase @Inject constructor(
    private val repo: RecordRepository
) {
    suspend operator fun invoke(req: RecordCreateRequest): Result<Long> =
        repo.createRecord(req)
}