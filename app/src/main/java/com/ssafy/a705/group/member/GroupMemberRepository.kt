package com.ssafy.a705.group.member

import com.ssafy.a705.network.GroupApiService
import javax.inject.Inject
import javax.inject.Singleton

interface GroupMemberRepository {
    suspend fun search(query: String): List<MemberSearchItem>
    suspend fun inviteByEmail(groupId: Long, email: String)
    suspend fun getMembers(groupId: Long): List<MemberItem>
}

@Singleton
class GroupMemberRepositoryImpl @Inject constructor(
    private val api: GroupApiService
) : GroupMemberRepository {

    override suspend fun search(query: String): List<MemberSearchItem> =
        api.searchMembers(query).data?.members ?: emptyList()

    override suspend fun inviteByEmail(groupId: Long, email: String) {
        api.inviteMemberByEmail(groupId, InviteMemberRequest(listOf(email)))
    }

    override suspend fun getMembers(groupId: Long): List<MemberItem> {
        val data = api.getGroupMembers(groupId).data ?: return emptyList()
        println("🔍 GroupMemberRepository - API 응답 데이터: $data")
        println("🔍 GroupMemberRepository - 멤버 수: ${data.groupMembers.size}")
        
        val result = data.groupMembers.map { dto ->
            println("🔍 GroupMemberRepository - 멤버 정보: nickname=${dto.nickname}, profileImageUrl=${dto.profileImageUrl}, role=${dto.role}")
            
            // 닉네임으로 회원 검색하여 이메일 정보 가져오기
            val email = try {
                val searchResult = api.searchMembers(dto.nickname)
                val memberInfo = searchResult.data?.members?.find { it.nickname == dto.nickname }
                memberInfo?.email
            } catch (e: Exception) {
                println("🔍 GroupMemberRepository - 이메일 검색 실패: ${e.message}")
                null
            }
            
            // GroupMemberDto의 email 필드도 업데이트
            val updatedDto = dto.copy(email = email)
            
            MemberItem(
                id = updatedDto.groupMemberId.toString(),
                name = updatedDto.nickname,
                email = updatedDto.email,
                settlementAmount = updatedDto.finalAmount,
                lateFee = updatedDto.lateFee,
                profileImageUrl = updatedDto.profileImageUrl,
                role = updatedDto.role.ifBlank { "MEMBER" }
            )
        }
        
        println("🔍 GroupMemberRepository - 변환된 MemberItem 목록:")
        result.forEach { member ->
            println("  - ${member.name}: email=${member.email}, profileImageUrl=${member.profileImageUrl}")
        }
        
        return result
    }
}
