package com.ssafy.a705.feature.mypage.domain.usecase

import com.ssafy.a705.feature.mypage.data.model.response.MyCommentsPageResponse
import com.ssafy.a705.feature.mypage.domain.repository.MyPageRepository
import javax.inject.Inject

class GetMyCommentsUseCase @Inject constructor(
    private val repository: MyPageRepository
) {
    suspend operator fun invoke(page: Int?, size: Int?): MyCommentsPageResponse {
        return repository.getMyComments(page, size)
    }
}