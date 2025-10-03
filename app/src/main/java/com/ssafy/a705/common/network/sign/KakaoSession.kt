package com.ssafy.a705.common.network.sign

data class KakaoSession(
    val kakaoId: Long,
    val email: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val kakaoAccessToken: String,
    val kakaoRefreshToken: String?,
    val kakaoExpiresAtEpochSec: Long?
)