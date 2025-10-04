package com.ssafy.a705.feature.auth.domain.usecase

import com.ssafy.a705.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LogOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() {
        return repository.logout()
    }
}