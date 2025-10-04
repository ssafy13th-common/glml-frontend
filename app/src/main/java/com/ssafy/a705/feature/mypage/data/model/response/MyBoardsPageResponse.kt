package com.ssafy.a705.feature.mypage.data.model.response

import com.ssafy.a705.feature.board.data.model.response.PostListResponse

data class MyBoardsPageResponse(
    val boards: PostListResponse,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)