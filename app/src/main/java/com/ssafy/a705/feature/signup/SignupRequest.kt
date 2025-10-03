package com.ssafy.a705.feature.signup

data class SignupRequest(
    val name: String,
    val nickname: String,
    val email: String,
    val password: String,
    val gender: String,         // MALE or FEMALE
    val profileImage: String? = null
)
