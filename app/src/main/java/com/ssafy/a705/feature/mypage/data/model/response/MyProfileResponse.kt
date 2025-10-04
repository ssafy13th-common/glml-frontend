package com.ssafy.a705.feature.mypage.data.model.response

data class MyProfileResponse(
    val profileUrl: String?,  // null 이면 기본 이미지
    val nickname: String,
    val email: String
)