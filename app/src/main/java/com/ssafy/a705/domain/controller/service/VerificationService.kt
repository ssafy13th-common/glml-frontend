package com.ssafy.a705.domain.controller.service

import com.ssafy.a705.global.network.ApiClient
import com.ssafy.a705.global.network.TokenManager
import com.ssafy.a705.global.network.verification.SmsSendReq
import com.ssafy.a705.global.network.verification.SmsVerifyReq
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerificationService @Inject constructor(
    private val api: ApiClient,
    private val tokenManager: TokenManager
) {
    private fun accessToken(): String =
        tokenManager.getServerAccessToken() ?: throw IllegalStateException("No AccessToken")

    suspend fun sendSms(phone: String) =
        api.verificationApi.sendSms(accessToken(), SmsSendReq(phone))

    suspend fun verifySms(phone: String, code: String) =
        api.verificationApi.verifySms(accessToken(), SmsVerifyReq(phone, code))
}