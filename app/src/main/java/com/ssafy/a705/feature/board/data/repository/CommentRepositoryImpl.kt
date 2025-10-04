package com.ssafy.a705.feature.board.data.repository

import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.feature.board.data.source.CommentRemoteDataSource
import com.ssafy.a705.feature.board.domain.repository.CommentRepository
import com.ssafy.a705.feature.model.req.CommentRequest
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    private val remoteDataSource: CommentRemoteDataSource
) : CommentRepository {

    override suspend fun writeComment(postId: Long, comment: CommentRequest) {
        val res = remoteDataSource.writeComment(postId, comment)
        res.message?.let { throw ApiException(it) }
        // 성공이면 Unit 반환
    }

    override suspend fun updateComment(boardId: Long, commentId: Long, request: CommentRequest) {
        val res = remoteDataSource.updateComment(boardId, commentId, request)
        res.message?.let { throw ApiException(it) }
    }

    override suspend fun deleteComment(boardId: Long, commentId: Long) {
        val res = remoteDataSource.deleteComment(boardId, commentId)
        res.message?.let { throw ApiException(it) }
    }
}