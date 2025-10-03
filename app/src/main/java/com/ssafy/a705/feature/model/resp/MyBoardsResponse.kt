package com.ssafy.a705.feature.model.resp




data class MyBoardsPageResponse(
    val boards: List<BoardDto>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)

// 기존과 동일 (필드명 서버에 맞게 유지)
data class BoardDto(
    val boardId: Long,
    val title: String,
    val content: String,
    val comments: Int
)