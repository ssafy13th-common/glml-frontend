package com.ssafy.a705.feature.auth.data.model.response

data class JwtResponse(
    val accessToken: String,
    val refreshToken: String
)
