package com.ssafy.a705.feature.mypage

import com.ssafy.a705.feature.board.data.model.BoardDto

data class MyBoardsPageResponse(
    val boards: List<BoardDto>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)