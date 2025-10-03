package com.ssafy.a705.feature.board.data.model

data class CommentDto(
    val id: Long,
    val content: String,
    val author: String,
    val authorEmail: String,
    val authorProfileUrl: String?,
    val updatedAt: String,
    val createdAt: String,
    val parentComment: Long?,
    val timestamp: String
)

data class MyCommentDto(
    val boardTitle: String,
    val boardId: Long,
    val commentId: Long,
    val content: String
)