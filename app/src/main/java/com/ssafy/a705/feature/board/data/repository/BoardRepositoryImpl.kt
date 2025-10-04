package com.ssafy.a705.feature.board.data.repository

import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.feature.board.data.model.CursorData
import com.ssafy.a705.feature.board.data.model.request.UpdatePostRequest
import com.ssafy.a705.feature.board.data.model.request.WritePostRequest
import com.ssafy.a705.feature.board.data.model.response.PostDetailResponse
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse
import com.ssafy.a705.feature.board.data.source.BoardRemoteDataSource
import com.ssafy.a705.feature.board.domain.repository.BoardRepository
import javax.inject.Inject

class BoardRepositoryImpl @Inject constructor(
    private val remoteDataSource: BoardRemoteDataSource
) : BoardRepository {

    override suspend fun getPosts(cursorId: Long?): CursorData {
        val res = remoteDataSource.getPosts(cursorId)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("게시글을 불러오는 중 문제가 생겼습니다.")
    }

    override suspend fun getPostDetail(postId: Long): PostDetailResponse {
        val res = remoteDataSource.getPostDetail(postId)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("게시물을 불러오는 중 문제가 생겼습니다.")
    }

    override suspend fun writePost(request: WritePostRequest): WritePostResponse {
        val res = remoteDataSource.writePost(request)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("새 글을 등록하는 중에 문제가 생겼습니다.")
    }

    override suspend fun updatePost(postId: Long, post: UpdatePostRequest) {
        val res = remoteDataSource.updatePost(postId, post)
        res.message?.let { throw ApiException(it) }
        // data는 null이어도 성공으로 간주
    }

    override suspend fun deletePost(postId: Long) {
        val res = remoteDataSource.deletePost(postId)
        res.message?.let { throw ApiException(it) }
    }
}
