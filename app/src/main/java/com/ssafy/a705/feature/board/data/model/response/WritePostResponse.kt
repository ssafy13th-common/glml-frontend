package com.ssafy.a705.feature.board.data.model.response

data class WritePostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val author: String,
    val authorProfile: String,
    val authorEmail: String
)