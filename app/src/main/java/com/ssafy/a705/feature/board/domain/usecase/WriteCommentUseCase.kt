package com.ssafy.a705.feature.board.domain.usecase

import com.ssafy.a705.feature.board.domain.repository.CommentRepository
import com.ssafy.a705.feature.board.data.model.request.CommentRequest
import javax.inject.Inject

class WriteCommentUseCase @Inject constructor(
    private val repository: CommentRepository
) {
    suspend operator fun invoke(postId: Long, comment: CommentRequest) {
        return repository.writeComment(postId, comment)
    }

}
