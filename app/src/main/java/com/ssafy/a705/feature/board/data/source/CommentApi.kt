package com.ssafy.a705.feature.board.data.source

import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.board.data.model.response.CommentResponse
import com.ssafy.a705.feature.model.req.CommentRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CommentApi {

    @POST("v1/boards/{postId}/comments")
    suspend fun writeComment(
        @Path("postId") postId: Long,
        @Body comment: CommentRequest
    ): BaseResponse<CommentResponse?>

    @PUT("v1/boards/{board-id}/comments/{comment-id}")
    suspend fun updateComment(
        @Path("board-id") boardId: Long,
        @Path("comment-id") commentId: Long,
        @Body request: CommentRequest
    ): BaseResponse<Unit>

    @DELETE("v1/boards/{board-id}/comments/{comment-id}")
    suspend fun deleteComment(
        @Path("board-id") postId: Long,
        @Path("comment-id") commentId: Long
    ): BaseResponse<Unit>
}