package com.ssafy.a705.model.base

data class BaseResponse<T>(
    val message: String?,
    val data: T?
)