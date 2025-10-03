package com.ssafy.a705.feature.signup

import com.ssafy.a705.feature.model.resp.BasicResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SignupApi {
    @POST("api/v1/auth/signup")
    suspend fun signup(@Body body: SignupRequest): BasicResponse

    @GET("api/v1/auth/email")
    suspend fun checkEmail(@Query("email") email: String): BasicResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<BasicResponse>

    @GET("api/v1/auth/verify")
    suspend fun verifyEmail(@Query("token") token: String): BasicResponse

    @POST("api/v1/auth/verify/resend")
    suspend fun resendVerify(@Body body: SignupEmailResendRequest): BasicResponse
}