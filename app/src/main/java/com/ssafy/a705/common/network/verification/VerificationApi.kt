package com.ssafy.a705.common.network.verification

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class SmsSendReq(val phoneNumber: String)
data class SmsVerifyReq(val phoneNumber: String, val certificationCode: String)
data class ApiResp<T>(val message: String?, val data: T?)

interface VerificationApi {
    @POST("api/v1/verification/sms")
    suspend fun sendSms(
        @Header("AccessToken") accessToken: String, // 서버 명세대로
        @Body req: SmsSendReq
    ): ApiResp<Unit>

    @POST("api/v1/verification/sms/verify")
    suspend fun verifySms(
        @Header("AccessToken") accessToken: String,
        @Body req: SmsVerifyReq
    ): ApiResp<Unit>
}