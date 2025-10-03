package com.ssafy.a705.feature.model.resp

data class MyProfileResponse(
    val profileUrl: String?,  // null 이면 기본 이미지
    val nickname: String,
    val email: String
)