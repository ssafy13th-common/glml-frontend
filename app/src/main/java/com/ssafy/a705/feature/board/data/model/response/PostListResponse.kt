package com.ssafy.a705.feature.board.data.model.response

data class PostListResponse(
    val id: Long,
    val title: String,
    val summary: String?,
    val author: String,
    val createdDate: String,
    val comments: Int
)
