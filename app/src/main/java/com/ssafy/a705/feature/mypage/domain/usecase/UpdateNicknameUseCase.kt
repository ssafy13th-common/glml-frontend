package com.ssafy.a705.feature.mypage.domain.usecase

import com.ssafy.a705.feature.mypage.data.model.request.PatchNicknameRequest
import com.ssafy.a705.feature.mypage.domain.repository.MyPageRepository
import javax.inject.Inject

class UpdateNicknameUseCase @Inject constructor(
    private val repository: MyPageRepository
) {
    suspend operator fun invoke(body: PatchNicknameRequest) {
        return repository.patchNickname(body)
    }
}