package com.ssafy.a705.feature.mypage.domain.usecase

import com.ssafy.a705.feature.mypage.data.model.response.MyProfileResponse
import com.ssafy.a705.feature.mypage.domain.repository.MyPageRepository
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val repository: MyPageRepository
) {
    suspend operator fun invoke(): MyProfileResponse {
        return repository.getMyProfile()
    }
}