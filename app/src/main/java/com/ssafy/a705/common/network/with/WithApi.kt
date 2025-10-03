package com.ssafy.a705.common.network.with

import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.model.req.CommentRequest
import com.ssafy.a705.feature.model.req.UpdatePostRequest
import com.ssafy.a705.feature.model.req.WithPostWriteRequest
import com.ssafy.a705.feature.model.resp.CommentResponse
import com.ssafy.a705.feature.model.resp.CursorData
import com.ssafy.a705.feature.model.resp.WithPostDetailData
import com.ssafy.a705.feature.model.resp.WithPostWriteResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface WithApi {

    @GET("/api/v1/boards/{boardId}")
    suspend fun getPostDetail(
        @Path("boardId") boardId: Long
    ): BaseResponse<WithPostDetailData>

    @GET("/api/v1/boards")
    suspend fun getWithPosts(
        @Query("cursorId") cursorId: Long? = null
    ): BaseResponse<CursorData>

    @POST("/api/v1/boards")
    suspend fun writePost(
        @Body request: WithPostWriteRequest
    ): BaseResponse<WithPostWriteResponse>

    @PUT("/api/v1/boards/{id}")
    suspend fun updatePost(
        @Path("id") postId: Long,
        @Body request: UpdatePostRequest
    ): BaseResponse<Unit>
    @DELETE("/api/v1/boards/{id}")
    suspend fun deletePost(
        @Path("id") postId: Long
    ): BaseResponse<Unit>

    @POST("/api/v1/boards/{postId}/comments")
    suspend fun writeComment(
        @Path("postId") postId: Long,
        @Body comment: CommentRequest
    ): BaseResponse<CommentResponse?>

    @PUT("/api/v1/boards/{board-id}/comments/{comment-id}")
    suspend fun updateComment(
        @Path("board-id") boardId: Long,
        @Path("comment-id") commentId: Long,
        @Body request: CommentRequest
    ): BaseResponse<Unit>


    @DELETE("/api/v1/boards/{board-id}/comments/{comment-id}")
    suspend fun deleteComment(
        @Path("board-id") postId: Long,
        @Path("comment-id") commentId: Long
    ): BaseResponse<Unit>

    //디버깅용
//    @GET("/api/v1/boards")
//    suspend fun getWithPostsRaw(
//        @Query("page") page: Int,
//        @Query("size") size: Int
//    ): okhttp3.ResponseBody
}