package com.ssafy.a705.with.model

data class Comment(
    val id: Int,
    val author: String,
    val content: String,
    val timestamp: String,
    val parentId: Long?,
    val replies: List<Comment> = emptyList()
)