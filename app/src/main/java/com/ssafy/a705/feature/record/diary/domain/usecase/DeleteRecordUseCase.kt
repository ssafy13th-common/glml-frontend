package com.ssafy.a705.feature.record.diary.domain.usecase

import com.ssafy.a705.feature.record.diary.domain.repository.RecordRepository
import javax.inject.Inject

class DeleteRecordUseCase @Inject constructor(
    private val repo: RecordRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> =
        repo.deleteRecord(id)
}