package com.ssafy.a705.feature.board.domain.repository

import com.ssafy.a705.feature.board.data.model.CursorData
import com.ssafy.a705.feature.board.data.model.request.UpdatePostRequest
import com.ssafy.a705.feature.board.data.model.request.WritePostRequest
import com.ssafy.a705.feature.board.data.model.response.PostDetailResponse
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse

interface BoardRepository {

    suspend fun getPosts(cursorId: Long?): CursorData

    suspend fun getPostDetail(postId: Long): PostDetailResponse

    suspend fun writePost(request: WritePostRequest): WritePostResponse

    suspend fun updatePost(postId: Long, post: UpdatePostRequest)

    suspend fun deletePost(postId: Long)
}