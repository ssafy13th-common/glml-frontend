package com.ssafy.a705.feature.board.domain.repository

import com.ssafy.a705.feature.board.data.model.request.PostRequest
import com.ssafy.a705.feature.board.data.model.response.PostDetailResponse
import com.ssafy.a705.feature.board.data.model.response.PostListResponse
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse

interface BoardRepository {

    suspend fun getPosts(cursorId: Long?): PostListResponse

    suspend fun getPostDetail(postId: Long): PostDetailResponse

    suspend fun writePost(request: PostRequest): WritePostResponse

    suspend fun updatePost(postId: Long, post: PostRequest)

    suspend fun deletePost(postId: Long)
}