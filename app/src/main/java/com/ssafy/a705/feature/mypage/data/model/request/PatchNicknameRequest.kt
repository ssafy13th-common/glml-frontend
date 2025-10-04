package com.ssafy.a705.feature.mypage.data.model.request

data class PatchNicknameRequest(
    val email: String,
    val nickname: String
)