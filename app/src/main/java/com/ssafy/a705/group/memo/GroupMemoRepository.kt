package com.ssafy.a705.group.memo

interface GroupMemoRepository {
    suspend fun getAllMemos(groupId: Long): List<MemoItem>
    suspend fun createMemo(groupId: Long, content: String)
    suspend fun updateMemo(groupId: Long, memoId: Long, request: MemoUpdateRequestDto)
    suspend fun deleteMemo(groupId: Long, memoId: Long)
}
