package com.ssafy.a705.feature.board.data.model.response

data class PostData(
    val id: Long,
    val title: String,
    val author: String,
    val summary: String,
    val updatedAt: String,
    val comments: Int
)
