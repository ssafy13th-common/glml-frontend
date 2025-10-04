package com.ssafy.a705.feature.board.data.source

import com.ssafy.a705.feature.board.data.model.request.UpdatePostRequest
import com.ssafy.a705.feature.board.data.model.request.WritePostRequest
import javax.inject.Inject

class BoardRemoteDataSource @Inject constructor(private val api: BoardApi) {
    suspend fun getPosts(cursorId: Long?) = api.getPosts(cursorId);
    suspend fun getPostDetail(postId: Long) = api.getPostDetail(postId)
    suspend fun writePost(request: WritePostRequest) = api.writePost(request)
    suspend fun updatePost(postId: Long, request: UpdatePostRequest) =
        api.updatePost(postId, request)

    suspend fun deletePost(postId: Long) = api.deletePost(postId)
}