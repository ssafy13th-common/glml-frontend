package com.ssafy.a705.feature.group.memo

data class MemoListEnvelope(
    val groupId: Long,
    val memos: List<MemoItem>
)

data class MemoItem(
    val memoId: Long,
    val content: String,
    val writer: String
)

data class MemoCreateRequest(
    val content: String
)

data class MemoUpdateRequestDto(
    val groupMemberId: Long,
    val content: String
)
