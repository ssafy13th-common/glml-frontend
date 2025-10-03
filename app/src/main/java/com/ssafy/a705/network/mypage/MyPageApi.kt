package com.ssafy.a705.network.mypage

import com.ssafy.a705.model.base.BaseResponse
import com.ssafy.a705.model.req.PatchNicknameRequest
import com.ssafy.a705.model.req.PatchProfileRequest
import com.ssafy.a705.model.resp.MyBoardsPageResponse
import com.ssafy.a705.model.resp.MyCommentsPageResponse
import com.ssafy.a705.model.resp.MyProfileResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface MypageApi {

    @GET("api/v1/mypage/boards")
    suspend fun getMyBoards(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): BaseResponse<MyBoardsPageResponse>

    @GET("api/v1/mypage/comments")
    suspend fun getMyComments(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): BaseResponse<MyCommentsPageResponse>

    @GET("api/v1/mypage/me")
    suspend fun getMyProfile(): BaseResponse<MyProfileResponse>

    @PATCH("api/v1/mypage/me/nickname")
    suspend fun patchNickname(
        @Body body: PatchNicknameRequest
    ): BaseResponse<Unit>

    @PATCH("api/v1/mypage/me/profile")
    suspend fun patchProfile(@Body body: PatchProfileRequest): BaseResponse<Unit>

    @POST("api/v1/auth/logout")
    suspend fun logout(): BaseResponse<Unit?>

    @DELETE("api/v1/auth/withdraw")
    suspend fun withdraw(): BaseResponse<Unit?>
}