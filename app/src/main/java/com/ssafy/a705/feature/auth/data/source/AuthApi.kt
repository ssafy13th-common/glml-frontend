package com.ssafy.a705.feature.auth.data.source

import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.auth.data.model.request.KakaoLoginRequest
import com.ssafy.a705.feature.auth.data.model.request.LoginRequest
import com.ssafy.a705.feature.auth.data.model.response.JwtResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<BaseResponse<JwtResponse>>

    @POST("v1/auth/logout")
    suspend fun logout(): Response<BaseResponse<Unit?>>

    @POST("v1/auth/kakao/mobile")
    suspend fun loginWithKakao(
        @Body request: KakaoLoginRequest
    ): Response<BaseResponse<JwtResponse>>


}