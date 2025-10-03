package com.ssafy.a705.group.member

import com.google.gson.annotations.SerializedName

// ── 멤버 검색/초대 DTO (기존 그대로 유지)
data class MemberSearchItem(
    val email: String,
    val nickname: String,
    @SerializedName(value = "profileUrl", alternate = ["profile"])
    val profileUrl: String?
)
data class MemberSearchData(val members: List<MemberSearchItem>)
data class InviteMemberRequest(val emails: List<String>)

// ── 그룹 멤버 목록 응답 컨테이너: data.group_id / members_count / group_members
data class GroupMembersData(
    val groupId: Long,
    val membersCount: Int,
    val groupMembers: List<GroupMemberDto>
)

// ── 개별 멤버 항목: id / profile_url / nickname / total_amount / late_fee
data class GroupMemberDto(
    val groupMemberId: Long,
    val role: String,
    @SerializedName(value = "profileImageUrl", alternate = ["profileUrl", "profile_image_url", "profile"])
    val profileImageUrl: String?,
    val nickname: String,
    val email: String?, // 이메일 필드 추가
    val finalAmount: Int,
    val lateFee: Int
)