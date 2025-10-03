package com.ssafy.a705.controller.service

import com.ssafy.a705.model.req.SignupRequest
import com.ssafy.a705.model.resp.SignupResponse
import com.ssafy.a705.global.network.sign.SignApi
import javax.inject.Inject

class SignupService @Inject constructor(
    private val signApi: SignApi
) {
    suspend fun signup(request: SignupRequest): SignupResponse {
        return signApi.signup(request)
    }
}