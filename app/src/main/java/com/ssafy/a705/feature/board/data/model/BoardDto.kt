package com.ssafy.a705.feature.board.data.model

data class BoardDto(
    val boardId: Long,
    val title: String,
    val content: String,
    val comments: Int
)