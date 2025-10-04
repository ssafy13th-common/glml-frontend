package com.ssafy.a705.feature.auth.data.source

import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.auth.data.model.request.KakaoLoginRequest
import com.ssafy.a705.feature.auth.data.model.response.JwtResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface AuthApi {

    @POST("v1/auth/logout")
    suspend fun logout(): BaseResponse<Unit?>

    @DELETE("v1/auth/withdrawal")
    suspend fun withdrawal(): BaseResponse<Unit?>

    @POST("v1/auth/kakao/mobile")
    suspend fun loginWithKakao(
        @Body req: KakaoLoginRequest
    ): BaseResponse<JwtResponse>
}