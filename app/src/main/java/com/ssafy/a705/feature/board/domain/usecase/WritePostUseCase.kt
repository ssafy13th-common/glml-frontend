package com.ssafy.a705.feature.board.domain.usecase

import com.ssafy.a705.feature.board.data.model.request.PostRequest
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse
import com.ssafy.a705.feature.board.domain.repository.BoardRepository
import javax.inject.Inject

class WritePostUseCase @Inject constructor(
    private val repository: BoardRepository
) {
    suspend operator fun invoke(post: PostRequest): WritePostResponse {
        return repository.writePost(post)
    }

}
