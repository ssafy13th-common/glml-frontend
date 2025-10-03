package com.ssafy.a705.feature.board.data.model.response

data class CommentResponse(
    val id: Long,
    val content: String,
    val author: String,
    val authorEmail: String,           // ← 추가
    val authorProfileUrl: String?,
    val timestamp: String,
    val parentId: Long?,                 // 루트면 null
    val replies: List<CommentResponse> = emptyList()
)

