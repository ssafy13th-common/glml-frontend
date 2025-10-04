package com.ssafy.a705.feature.board.domain.usecase

import com.ssafy.a705.feature.board.data.model.request.WritePostRequest
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse
import com.ssafy.a705.feature.board.domain.repository.BoardRepository
import com.ssafy.a705.feature.board.domain.repository.CommentRepository
import com.ssafy.a705.feature.model.req.CommentRequest
import javax.inject.Inject

class WritePostUseCase @Inject constructor(
    private val repository: BoardRepository
) {
    suspend operator fun invoke(post: WritePostRequest): WritePostResponse {
        return repository.writePost(post)
    }

}
