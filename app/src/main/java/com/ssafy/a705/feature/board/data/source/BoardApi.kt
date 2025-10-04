package com.ssafy.a705.feature.board.data.source

import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.board.data.model.CursorData
import com.ssafy.a705.feature.board.data.model.request.UpdatePostRequest
import com.ssafy.a705.feature.board.data.model.request.WritePostRequest
import com.ssafy.a705.feature.board.data.model.response.PostDetailResponse
import com.ssafy.a705.feature.board.data.model.response.WritePostResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BoardApi {

    @GET("v1/boards/{boardId}")
    suspend fun getPostDetail(
        @Path("boardId") boardId: Long
    ): BaseResponse<PostDetailResponse>

    @GET("v1/boards")
    suspend fun getPosts(
        @Query("cursorId") cursorId: Long? = null
    ): BaseResponse<CursorData>

    @POST("v1/boards")
    suspend fun writePost(
        @Body request: WritePostRequest
    ): BaseResponse<WritePostResponse>

    @PUT("v1/boards/{id}")
    suspend fun updatePost(
        @Path("id") postId: Long,
        @Body request: UpdatePostRequest
    ): BaseResponse<Unit>

    @DELETE("v1/boards/{id}")
    suspend fun deletePost(
        @Path("id") postId: Long
    ): BaseResponse<Unit>
}