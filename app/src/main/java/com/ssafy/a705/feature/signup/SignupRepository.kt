package com.ssafy.a705.feature.signup

import com.ssafy.a705.feature.model.resp.BasicResponse
import retrofit2.Response
import javax.inject.Inject

class SignupRepository @Inject constructor(
    private val api: SignupApi
) {
    suspend fun signup(req: SignupRequest): BasicResponse = api.signup(req)

    suspend fun checkEmail(email: String): BasicResponse = api.checkEmail(email)

    suspend fun login(email: String, password: String): Response<BasicResponse> =
        api.login(LoginRequest(email, password))

    suspend fun verifyEmail(token: String): BasicResponse = api.verifyEmail(token)

    suspend fun resendVerify(email: String): BasicResponse =
        api.resendVerify(SignupEmailResendRequest(email))
}