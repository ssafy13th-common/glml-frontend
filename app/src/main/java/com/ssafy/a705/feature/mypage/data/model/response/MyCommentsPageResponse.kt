package com.ssafy.a705.feature.mypage.data.model.response

import com.ssafy.a705.feature.board.data.model.MyCommentDto


data class MyCommentsPageResponse(
    val comments: List<MyCommentDto>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)

