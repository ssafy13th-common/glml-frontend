package com.ssafy.a705.feature.board.data.source

import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.board.data.model.CursorData
import com.ssafy.a705.feature.board.data.model.response.CommentResponse
import com.ssafy.a705.feature.board.data.model.request.UpdatePostRequest
import com.ssafy.a705.feature.board.data.model.request.WritePostRequest
import com.ssafy.a705.feature.board.data.model.response.PostDetailResponse
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse
import com.ssafy.a705.feature.model.req.CommentRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BoardApi {

    @GET("/v1/boards/{boardId}")
    suspend fun getPostDetail(
        @Path("boardId") boardId: Long
    ): BaseResponse<PostDetailResponse>

    @GET("/v1/boards")
    suspend fun getPost(
        @Query("cursorId") cursorId: Long? = null
    ): BaseResponse<CursorData>

    @POST("/v1/boards")
    suspend fun writePost(
        @Body request: WritePostRequest
    ): BaseResponse<WritePostResponse>

    @PUT("/v1/boards/{id}")
    suspend fun updatePost(
        @Path("id") postId: Long,
        @Body request: UpdatePostRequest
    ): BaseResponse<Unit>
    @DELETE("/v1/boards/{id}")
    suspend fun deletePost(
        @Path("id") postId: Long
    ): BaseResponse<Unit>

    @POST("/v1/boards/{postId}/comments")
    suspend fun writeComment(
        @Path("postId") postId: Long,
        @Body comment: CommentRequest
    ): BaseResponse<CommentResponse?>

    @PUT("/v1/boards/{board-id}/comments/{comment-id}")
    suspend fun updateComment(
        @Path("board-id") boardId: Long,
        @Path("comment-id") commentId: Long,
        @Body request: CommentRequest
    ): BaseResponse<Unit>

    @DELETE("/v1/boards/{board-id}/comments/{comment-id}")
    suspend fun deleteComment(
        @Path("board-id") postId: Long,
        @Path("comment-id") commentId: Long
    ): BaseResponse<Unit>
}