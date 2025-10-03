package com.ssafy.a705.feature.board.data.repository

import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.feature.board.data.model.CursorData
import com.ssafy.a705.feature.board.data.model.request.UpdatePostRequest
import com.ssafy.a705.feature.board.data.model.request.WritePostRequest
import com.ssafy.a705.feature.board.data.model.response.PostDetailResponse
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse
import com.ssafy.a705.feature.board.data.source.BoardRemoteDataSource
import com.ssafy.a705.feature.model.req.CommentRequest
import javax.inject.Inject

class BoardRepository @Inject constructor(
    private val remoteDataSource: BoardRemoteDataSource
) {

    // 1) 게시판 전체 조회
    suspend fun getWithPosts(cursorId: Long?): CursorData {
        val res = remoteDataSource.getPost(cursorId)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("게시글을 불러오는 중 문제가 생겼습니다.")
    }

    // 2) 상세 조회
    suspend fun getPostDetail(postId: Long): PostDetailResponse {
        val res = remoteDataSource.getPostDetail(postId)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("게시물을 불러오는 중 문제가 생겼습니다.")
    }

    // 3) 글 작성
    suspend fun writePost(request: WritePostRequest): WritePostResponse {
        val res = remoteDataSource.writePost(request)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("새 글을 등록하는 중에 문제가 생겼습니다.")
    }

    // 4) 글 수정
    suspend fun updatePost(postId: Long, title: String, content: String) {
        val res = remoteDataSource.updatePost(postId, UpdatePostRequest(title, content))
        res.message?.let { throw ApiException(it) }
        // data는 null이어도 성공으로 간주
    }

    // 5) 글 삭제
    suspend fun deletePost(postId: Long) {
        val res = remoteDataSource.deletePost(postId)
        res.message?.let { throw ApiException(it) }
    }

    // 6) 댓글 작성
    suspend fun writeComment(postId: Long, comment: CommentRequest) {
        val res = remoteDataSource.writeComment(postId, comment)
        res.message?.let { throw ApiException(it) }
        // 성공이면 Unit 반환
    }

    // 7) 댓글 수정
    suspend fun updateComment(boardId: Long, commentId: Long, request: CommentRequest) {
        val res = remoteDataSource.updateComment(boardId, commentId, request)
        res.message?.let { throw ApiException(it) }
    }

    // 8) 댓글 삭제
    suspend fun deleteComment(boardId: Long, commentId: Long) {
        val res = remoteDataSource.deleteComment(boardId, commentId)
        res.message?.let { throw ApiException(it) }
    }
}
