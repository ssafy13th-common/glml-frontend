package com.ssafy.a705.chatSet.dto

data class ApiEnvelope<T>(
    val message: String? = null,
    val data: T? = null
)