package com.ssafy.a705.feature.board.domain.usecase

import com.ssafy.a705.feature.board.data.model.CursorData
import com.ssafy.a705.feature.board.domain.repository.BoardRepository
import javax.inject.Inject

class DeletePostUseCase @Inject constructor(
    private val repository: BoardRepository
) {
    suspend operator fun invoke(postId: Long) {
        return repository.deletePost(postId)
    }
}
