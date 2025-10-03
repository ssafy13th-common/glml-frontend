package com.ssafy.a705.group.memo

import com.ssafy.a705.global.network.GroupApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupMemoRepositoryImpl @Inject constructor(
    private val api: GroupApiService
) : GroupMemoRepository {

    override suspend fun getAllMemos(groupId: Long): List<MemoItem> {
        return try {
            val response = api.getGroupMemos(groupId)
            if (response.data != null) {
                // 메모 ID를 기준으로 내림차순 정렬 (최신 메모가 맨 위에 오도록)
                response.data.memos.sortedByDescending { it.memoId }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            // API 에러 로깅
            println("메모 조회 실패: ${e.message}")
            emptyList()
        }
    }

    override suspend fun createMemo(groupId: Long, content: String) {
        try {
            api.createGroupMemo(groupId, MemoCreateRequest(content))
        } catch (e: Exception) {
            println("메모 생성 실패: ${e.message}")
            throw e
        }
    }

    override suspend fun updateMemo(groupId: Long, memoId: Long, request: MemoUpdateRequestDto) {
        try {
            api.updateGroupMemo(groupId, memoId, request)
        } catch (e: Exception) {
            println("메모 수정 실패: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteMemo(groupId: Long, memoId: Long) {
        try {
            api.deleteGroupMemo(groupId, memoId)
        } catch (e: Exception) {
            println("메모 삭제 실패: ${e.message}")
            throw e
        }
    }
}
