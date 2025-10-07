package com.ssafy.a705.feature.board.domain.usecase

import com.ssafy.a705.feature.board.data.model.response.PostListResponse
import com.ssafy.a705.feature.board.domain.repository.BoardRepository
import javax.inject.Inject

class GetPostsUseCase @Inject constructor(
    private val repository: BoardRepository
) {
    suspend operator fun invoke(cursor: Long?): PostListResponse {
        return repository.getPosts(cursor)
    }
}
