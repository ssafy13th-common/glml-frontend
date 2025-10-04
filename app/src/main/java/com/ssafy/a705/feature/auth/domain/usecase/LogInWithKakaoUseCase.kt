package com.ssafy.a705.feature.auth.domain.usecase

import com.ssafy.a705.feature.auth.data.model.request.KakaoLoginRequest
import com.ssafy.a705.feature.auth.data.model.response.JwtResponse
import com.ssafy.a705.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LogInWithKakaoUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(request: KakaoLoginRequest): JwtResponse {
        return repository.loginWithKakao(request)
    }
}