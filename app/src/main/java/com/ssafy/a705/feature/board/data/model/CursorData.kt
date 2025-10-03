package com.ssafy.a705.feature.board.data.model

import com.ssafy.a705.feature.board.data.model.response.PostData

data class CursorData(
    val boards: List<PostData>,
    val nextCursor: Long?
)
