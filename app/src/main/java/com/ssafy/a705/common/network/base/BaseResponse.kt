package com.ssafy.a705.common.network.base

data class BaseResponse<T>(
    val message: String?,
    val data: T?
)