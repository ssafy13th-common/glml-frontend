package com.ssafy.a705.feature.board.data.source

import com.ssafy.a705.feature.model.req.CommentRequest
import javax.inject.Inject

class CommentRemoteDataSource @Inject constructor(private val api: CommentApi) {
    suspend fun writeComment(postId: Long, comment: CommentRequest) =
        api.writeComment(postId, comment)

    suspend fun updateComment(boardId: Long, commentId: Long, request: CommentRequest) =
        api.updateComment(boardId, commentId, request)

    suspend fun deleteComment(boardId: Long, commentId: Long) =
        api.deleteComment(boardId, commentId)
}