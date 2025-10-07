package com.ssafy.a705.feature.record.diary.domain.usecase

import androidx.paging.PagingSource
import com.ssafy.a705.feature.record.diary.data.model.RecordListItem
import com.ssafy.a705.feature.record.diary.domain.repository.RecordRepository
import javax.inject.Inject

class GetDiaryPagingUseCase @Inject constructor(
    private val repo: RecordRepository
) {
    operator fun invoke(
        pageSize: Int,
        locationCode: Int?
    ): PagingSource<Long, RecordListItem> = repo.diaryPagingSource(pageSize, locationCode)
}