package com.ssafy.a705.feature.mypage.domain.usecase

import com.ssafy.a705.feature.mypage.domain.repository.MyPageRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val repository: MyPageRepository
) {
    suspend operator fun invoke(email: String, profileUrlOrKey: String) {
        return repository.patchProfile(email, profileUrlOrKey)
    }
}