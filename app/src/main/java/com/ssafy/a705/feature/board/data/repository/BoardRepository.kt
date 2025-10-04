package com.ssafy.a705.feature.board.data.repository

import com.ssafy.a705.feature.board.data.model.CursorData
import com.ssafy.a705.feature.board.data.model.request.WritePostRequest
import com.ssafy.a705.feature.board.data.model.response.PostDetailResponse
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse
import com.ssafy.a705.feature.model.req.CommentRequest

interface BoardRepository {

    suspend fun getPosts(cursorId: Long?): CursorData

    suspend fun getPostDetail(postId: Long): PostDetailResponse

    suspend fun writePost(request: WritePostRequest): WritePostResponse

    suspend fun updatePost(postId: Long, title: String, content: String)

    suspend fun deletePost(postId: Long)

    suspend fun writeComment(postId: Long, comment: CommentRequest)

    suspend fun updateComment(boardId: Long, commentId: Long, request: CommentRequest)

    suspend fun deleteComment(boardId: Long, commentId: Long)
}
