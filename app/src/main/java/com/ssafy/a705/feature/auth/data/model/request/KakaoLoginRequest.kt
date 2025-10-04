package com.ssafy.a705.feature.auth.data.model.request

import com.ssafy.a705.common.network.sign.SignApi

data class KakaoLoginRequest(
    val accessToken: String,
    val gender: SignApi.Gender,
    val name: String,
)
