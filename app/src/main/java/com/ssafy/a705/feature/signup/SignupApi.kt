package com.ssafy.a705.feature.signup

import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.auth.data.model.request.SignupEmailResendRequest
import com.ssafy.a705.common.model.BasicResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SignupApi {
    @POST("api/v1/auth/signup")
    suspend fun signup(@Body body: SignupRequest): BasicResponse

    @DELETE("v1/auth/withdrawal")
    suspend fun withdrawal(): BaseResponse<Unit?>

    @GET("v1/auth/email")
    suspend fun checkEmail(
        @Query("email") email: String
    ): BaseResponse<Unit?>

    @GET("v1/auth/verify")
    suspend fun verifyEmail(
        @Query("token") token: String
    ): BasicResponse

    @POST("v1/auth/verify/resend")
    suspend fun resendVerify(
        @Body body: SignupEmailResendRequest
    ): BasicResponse
}