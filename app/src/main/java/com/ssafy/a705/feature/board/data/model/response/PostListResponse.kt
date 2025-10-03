package com.ssafy.a705.feature.board.data.model.response

data class PostListResponse(
    val boards: List<PostData>,
    val nextCursor: Long? = null
)
