package com.ssafy.a705.model.resp

data class WithPostWriteResponse(
    val id: Long,
    val title: String,
    val content: String,
    val author: String,
    val authorProfile: String,
    val authorEmail: String
)