package com.ssafy.a705.model.resp


data class WithPostDetailData(
    val title: String,
    val content: String,
    val author: String,
    val authorProfileUrl: String?,
    val authorEmail: String,
    val updatedDate: String,
    val comments: List<CommentDto>
)

data class WithComment(
    val id: Long,
    val content: String,
    val author: String,
    val authorProfileUrl: String,
    val authorEmail: String,
    val updatedDate: String,
    val parentComment: Long?
)