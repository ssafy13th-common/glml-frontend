package com.ssafy.a705.model.resp

data class CursorData(
    val boards: List<WithPostDto>,
    val nextCursor: Long?
)


data class WithPostDto(
    val id: Long,
    val title: String,
    val summary: String?,
    val author: String,
    val createdDate: String,
    val comments: Int
)