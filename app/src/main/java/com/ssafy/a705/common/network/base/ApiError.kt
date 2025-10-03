package com.ssafy.a705.common.network.base

data class ApiError(
    val message: String? = null,
    val data: Any? = null
)