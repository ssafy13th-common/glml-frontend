package com.ssafy.a705.domain.group.common.model

data class Memo(
    val id: Int,
    var content: String,
    var isEditing: Boolean = false,  // 기본값을 false로 변경
    val isMine: Boolean = true,
    val writer: String = ""
)
