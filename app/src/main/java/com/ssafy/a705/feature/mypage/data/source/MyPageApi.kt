package com.ssafy.a705.feature.mypage.data.source

import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.mypage.data.model.request.PatchNicknameRequest
import com.ssafy.a705.feature.mypage.data.model.request.PatchProfileRequest
import com.ssafy.a705.feature.mypage.data.model.response.MyBoardsPageResponse
import com.ssafy.a705.feature.mypage.data.model.response.MyCommentsPageResponse
import com.ssafy.a705.feature.mypage.data.model.response.MyProfileResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Query

interface MyPageApi {

    @GET("v1/mypage/boards")
    suspend fun getMyBoards(
        @Query("page") page: Int? = 0,
        @Query("size") size: Int? = 10
    ): BaseResponse<MyBoardsPageResponse>

    @GET("v1/mypage/comments")
    suspend fun getMyComments(
        @Query("page") page: Int? = 0,
        @Query("size") size: Int? = 10
    ): BaseResponse<MyCommentsPageResponse>

    @GET("v1/mypage/me")
    suspend fun getMyProfile(): BaseResponse<MyProfileResponse>

    @PATCH("v1/mypage/me/nickname")
    suspend fun patchNickname(
        @Body body: PatchNicknameRequest
    ): BaseResponse<Unit>

    @PATCH("v1/mypage/me/profile")
    suspend fun patchProfile(@Body body: PatchProfileRequest): BaseResponse<Unit>

}