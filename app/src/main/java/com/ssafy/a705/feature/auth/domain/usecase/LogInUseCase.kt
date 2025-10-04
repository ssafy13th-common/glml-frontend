package com.ssafy.a705.feature.auth.domain.usecase

import com.ssafy.a705.feature.auth.data.model.request.KakaoLoginRequest
import com.ssafy.a705.feature.auth.data.model.request.LoginRequest
import com.ssafy.a705.feature.auth.data.model.response.JwtResponse
import com.ssafy.a705.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LogInUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(request: LoginRequest): JwtResponse {
        return repository.login(request)
    }
}