package com.ssafy.a705.feature.auth.domain.repository

import com.ssafy.a705.feature.auth.data.model.request.KakaoLoginRequest
import com.ssafy.a705.feature.auth.data.model.request.LoginRequest
import com.ssafy.a705.feature.auth.data.model.response.JwtResponse

interface AuthRepository {
    suspend fun login(request: LoginRequest): JwtResponse
    suspend fun logout()
    suspend fun loginWithKakao(request: KakaoLoginRequest): JwtResponse

}