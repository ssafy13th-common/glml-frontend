package com.ssafy.a705.feature.model.resp


data class MyCommentsPageResponse(
    val comments: List<MyCommentDto>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class MyCommentDto(
    val boardTitle: String,
    val boardId: Long,
    val commentId: Long,
    val content: String
)