package com.ssafy.a705.common.network.sign

import com.google.gson.annotations.SerializedName
import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.model.req.SignupRequest
import com.ssafy.a705.feature.model.resp.SignupResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface SignApi {

    @POST("/v1/members/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): SignupResponse

    enum class Gender { MALE, FEMALE }
}