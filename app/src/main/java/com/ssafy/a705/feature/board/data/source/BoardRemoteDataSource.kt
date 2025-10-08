package com.ssafy.a705.feature.board.data.source

import android.util.Log
import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.board.data.model.request.PostRequest
import com.ssafy.a705.feature.board.data.model.response.PostDetailResponse
import com.ssafy.a705.feature.board.data.model.response.PostListResponse
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse
import javax.inject.Inject

class BoardRemoteDataSource @Inject constructor(private val api: BoardApi) {

    suspend fun getPosts(cursorId: Long?): BaseResponse<PostListResponse> {
        try {
            val response = api.getPosts(cursorId);
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("응답 비어있음")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("서버 오류 $code: $msg")
            }
        } catch (e: Exception) {
            Log.e("BOARD", "게시물 전체 조회 중 오류 발생: ${e.message}")
            throw e
        }
    }

    suspend fun getPostDetail(postId: Long): BaseResponse<PostDetailResponse> {
        try {
            val response = api.getPostDetail(postId)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("응답 비어있음")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("서버 오류 $code: $msg")
            }
        } catch (e: Exception) {
            Log.e("BOARD", "게시물 상세 조회 중 오류 발생: ${e.message}")
            throw e
        }
    }

    suspend fun writePost(request: PostRequest): BaseResponse<WritePostResponse> {
        try {
            val response = api.writePost(request)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("응답 비어있음")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("서버 오류 $code: $msg")
            }
        } catch (e: Exception) {
            Log.e("BOARD", "게시물 작성 중 오류 발생: ${e.message}")
            throw e
        }
    }

    suspend fun updatePost(postId: Long, request: PostRequest): BaseResponse<Unit> {
        try {
            val response = api.updatePost(postId, request)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("응답 비어있음")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("서버 오류 $code: $msg")
            }
        } catch (e: Exception) {
            Log.e("BOARD", "게시물 수정 중 오류 발생: ${e.message}")
            throw e
        }
    }

    suspend fun deletePost(postId: Long): BaseResponse<Unit> {
        try {
            val response = api.deletePost(postId)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiException("응답 비어있음")
                return body
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
                throw ApiException("서버 오류 $code: $msg")
            }
        } catch (e: Exception) {
            Log.e("BOARD", "게시물 삭제 중 오류 발생: ${e.message}")
            throw e
        }
    }
}