package com.ssafy.a705.domain.group.member

data class MemberItem(
    val id: String,           // 멤버 고유 ID
    val name: String,         // 닉네임
    val email: String? = null, // 이메일 필드 추가
    val profileImageUrl: String? = null, // 프로필 URL (없으면 기본 이미지)
    val settlementAmount: Int = 0,       // 정산비
    val lateFee: Int = 0,                // 지각비 (0이면 표시 안 함)
    val role: String = "MEMBER"          // 역할 (ADMIN, MEMBER 등)
)
