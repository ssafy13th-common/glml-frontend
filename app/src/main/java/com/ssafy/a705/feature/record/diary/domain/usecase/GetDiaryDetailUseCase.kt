package com.ssafy.a705.feature.record.diary.domain.usecase

import com.ssafy.a705.feature.record.diary.data.model.RecordDetailItem
import com.ssafy.a705.feature.record.diary.domain.repository.RecordRepository
import javax.inject.Inject

class GetDiaryDetailUseCase @Inject constructor(
    private val repo: RecordRepository
) {
    suspend operator fun invoke(id: Long): Result<RecordDetailItem> =
        repo.getDiaryDetail(id)
}