package com.ssafy.a705.feature.mypage.domain.usecase

import com.ssafy.a705.feature.mypage.data.model.response.MyBoardsPageResponse
import com.ssafy.a705.feature.mypage.domain.repository.MyPageRepository
import javax.inject.Inject

class GetMyPostsUseCase @Inject constructor(
    private val repository: MyPageRepository
) {
    suspend operator fun invoke(page: Int?, size: Int?): MyBoardsPageResponse {
        return repository.getMyPosts(page, size)
    }
}