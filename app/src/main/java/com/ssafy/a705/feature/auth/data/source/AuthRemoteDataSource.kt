package com.ssafy.a705.feature.auth.data.source

import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.common.network.base.BaseResponse
import com.ssafy.a705.feature.auth.data.model.request.KakaoLoginRequest
import com.ssafy.a705.feature.auth.data.model.request.LoginRequest
import com.ssafy.a705.feature.auth.data.model.response.JwtResponse
import javax.inject.Inject

class AuthRemoteDataSource @Inject constructor(private val api: AuthApi) {

    suspend fun login(request: LoginRequest): BaseResponse<JwtResponse> {
        val response = api.login(request)
        if (response.isSuccessful) {
            return response.body() ?: throw ApiException("로그인 응답이 비어있습니다.")
        } else {
            val code = response.code()
            val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
            throw ApiException("로그인 실패: $code - $msg")
        }
    }

    suspend fun loginWithKakao(request: KakaoLoginRequest): BaseResponse<JwtResponse> {
        val response = api.loginWithKakao(request)
        if (response.isSuccessful) {
            return response.body() ?: throw ApiException("카카오 로그인 응답이 비어있습니다.")
        } else {
            val code = response.code()
            val msg = response.errorBody()?.string() ?: "알 수 없는 오류"
            throw ApiException("카카오 로그인 실패: $code - $msg")
        }
    }

    suspend fun logout() {
        val response = api.logout()
        if (!response.isSuccessful) {
            val code = response.code()
            val msg = response.errorBody()?.string() ?: "알 수 없는 오류 발생"
            throw ApiException("로그아웃 실패: $code - $msg")
        }
    }
}
