package com.ssafy.a705.feature.model.req

data class SignupRequest(
    val name: String,
    val nickname: String,
    val email: String,
    val password: String,
    val gender: String
)