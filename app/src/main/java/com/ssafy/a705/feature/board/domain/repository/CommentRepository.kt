package com.ssafy.a705.feature.board.domain.repository

import com.ssafy.a705.feature.board.data.model.CursorData
import com.ssafy.a705.feature.board.data.model.request.WritePostRequest
import com.ssafy.a705.feature.board.data.model.response.PostDetailResponse
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse
import com.ssafy.a705.feature.model.req.CommentRequest

interface CommentRepository {

    suspend fun writeComment(postId: Long, comment: CommentRequest)

    suspend fun updateComment(boardId: Long, commentId: Long, request: CommentRequest)

    suspend fun deleteComment(boardId: Long, commentId: Long)
}