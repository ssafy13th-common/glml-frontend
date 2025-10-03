package com.ssafy.a705.feature.controller.service

import com.ssafy.a705.feature.model.req.SignupRequest
import com.ssafy.a705.feature.model.resp.SignupResponse
import com.ssafy.a705.common.network.sign.SignApi
import javax.inject.Inject

class SignupService @Inject constructor(
    private val signApi: SignApi
) {
    suspend fun signup(request: SignupRequest): SignupResponse {
        return signApi.signup(request)
    }
}