package com.ssafy.a705.feature.signup

import com.ssafy.a705.feature.auth.data.model.request.LoginRequest
import com.ssafy.a705.feature.auth.data.model.request.SignupEmailResendRequest
import com.ssafy.a705.feature.model.resp.BasicResponse
import javax.inject.Inject

class SignupRepository @Inject constructor(
    private val api: SignupApi
) {
    suspend fun signup(req: SignupRequest): BasicResponse = api.signup(req)

    suspend fun verifyEmail(token: String): BasicResponse = api.verifyEmail(token)

    suspend fun resendVerify(email: String): BasicResponse =
        api.resendVerify(SignupEmailResendRequest(email))

    suspend fun checkEmail(email: String) = api.checkEmail(email)
}