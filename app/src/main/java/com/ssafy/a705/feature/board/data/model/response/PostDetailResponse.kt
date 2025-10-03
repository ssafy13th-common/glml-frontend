package com.ssafy.a705.feature.board.data.model.response

import com.ssafy.a705.feature.board.data.model.CommentDto

data class PostDetailResponse(
    val title: String,
    val content: String,
    val author: String,
    val authorProfileUrl: String?,
    val authorEmail: String,
    val updatedAt: String,
    val comments: List<CommentDto>
)