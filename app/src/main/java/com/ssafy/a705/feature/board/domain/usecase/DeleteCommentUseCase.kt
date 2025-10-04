package com.ssafy.a705.feature.board.domain.usecase

import com.ssafy.a705.feature.board.domain.repository.CommentRepository
import com.ssafy.a705.feature.model.req.CommentRequest
import javax.inject.Inject

class DeleteCommentUseCase @Inject constructor(
    private val repository: CommentRepository
) {
    suspend operator fun invoke(postId: Long, commentId: Long) {
        return repository.deleteComment(postId, commentId)
    }

}
