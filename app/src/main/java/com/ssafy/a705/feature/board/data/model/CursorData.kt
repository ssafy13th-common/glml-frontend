package com.ssafy.a705.feature.board.data.model

import com.ssafy.a705.feature.board.data.model.response.PostListResponse

data class CursorData(
    val boards: List<PostListResponse>,
    val nextCursor: Long?
)
