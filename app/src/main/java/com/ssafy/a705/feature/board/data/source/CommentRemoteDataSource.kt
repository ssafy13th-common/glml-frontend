package com.ssafy.a705.feature.board.data.source

import android.util.Log
import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.board.data.model.response.CommentResponse
import com.ssafy.a705.feature.model.req.CommentRequest
import javax.inject.Inject

class CommentRemoteDataSource @Inject constructor(private val api: CommentApi) {

    suspend fun writeComment(postId: Long, comment: CommentRequest): BaseResponse<CommentResponse> {
        try {
            val response = api.writeComment(postId, comment)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("응답 비어있음")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("서버 오류 $code: $msg")
            }
        } catch (e: Exception) {
            Log.e("COMMENT", "댓글 작성 중 오류 발생: ${e.message}")
            throw e
        }
    }

    suspend fun updateComment(
        boardId: Long,
        commentId: Long,
        request: CommentRequest
    ): BaseResponse<Unit> {
        try {
            val response = api.updateComment(boardId, commentId, request)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("응답 비어있음")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("서버 오류 $code: $msg")
            }
        } catch (e: Exception) {
            Log.e("COMMENT", "댓글 수정 중 오류 발생: ${e.message}")
            throw e
        }
    }

    suspend fun deleteComment(boardId: Long, commentId: Long): BaseResponse<Unit> {
        try {
            val response = api.deleteComment(boardId, commentId)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("응답 비어있음")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("서버 오류 $code: $msg")
            }
        } catch (e: Exception) {
            Log.e("COMMENT", "댓글 삭제 중 오류 발생: ${e.message}")
            throw e
        }
    }
}