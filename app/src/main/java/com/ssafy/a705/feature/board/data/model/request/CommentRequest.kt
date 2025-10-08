package com.ssafy.a705.feature.board.data.model.request

data class CommentRequest(
    val content: String,
    val parentId: Long? = null
)