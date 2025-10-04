package com.ssafy.a705.feature.board.domain.usecase

import com.ssafy.a705.feature.board.data.model.request.UpdatePostRequest
import com.ssafy.a705.feature.board.domain.repository.BoardRepository
import javax.inject.Inject

class UpdatePostUseCase @Inject constructor(
    private val repository: BoardRepository
) {
    suspend operator fun invoke(postId: Long, post: UpdatePostRequest) {
        return repository.updatePost(postId, post)
    }
}
