package com.ssafy.a705.network.sign

import com.google.gson.annotations.SerializedName
import com.ssafy.a705.model.base.BaseResponse
import com.ssafy.a705.model.req.SignupRequest
import com.ssafy.a705.model.resp.SignupResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface SignApi {

    @POST("api/v1/members/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): SignupResponse

    //at와 jwt교환
    @POST("api/v1/auth/kakao/mobile")
    suspend fun loginWithKakao(
        @Body req: KakaoLoginRequest
    ): BaseResponse<JwtResponse>   // ← 래핑

    enum class Gender { MALE, FEMALE }
    data class KakaoLoginRequest(
        @SerializedName("accessToken")   // ← 명세서와 일치
        val accessToken: String,
        @SerializedName("gender")
        val gender: Gender,
        @SerializedName("name")
        val name: String,
    )

    data class JwtResponse(
        val accessToken: String,  // 명세서대로 "Bearer ..."가 올 수도 있음
        val refreshToken: String
    )
}