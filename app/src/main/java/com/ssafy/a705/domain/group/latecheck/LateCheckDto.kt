package com.ssafy.a705.domain.group.latecheck

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// --- 시간 포맷 도우미(밀리초 고정, Z 없음) ---
object TimeFmt {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    fun nowKst(): String =
        LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(fmt)
}

// --- WebSocket DTO ---
data class LiveLocationSend(
    val groupId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String // ISO-8601
)

/**
 * 서버에서 브로드캐스트되는 실시간 위치 정보
 * Redis에서 받아오는 데이터와 정확히 일치해야 함
 *
 * 예시 JSON:
 * {
 *   "groupId": 2,
 *   "memberEmail": "leedy903@naver.com",
 *   "latitude": 37.5550192,
 *   "longitude": 126.8985821,
 *   "timestamp": "2025-08-17T12:17:04.265",
 *   "lateFee": 1307000
 * }
 */
data class LiveLocationReceived(
    val groupId: Long,           // 그룹 ID
    val memberEmail: String,     // 멤버 이메일 (고유 식별자)
    val latitude: Double,        // 위도
    val longitude: Double,       // 경도
    val timestamp: String,       // ISO-8601 형식 타임스탬프
    val lateFee: Int            // 지각비 (원 단위)
)

// --- UI State ---
data class LateCheckUiState(
    val isLoading: Boolean = false,
    val groupMembers: List<MemberStatus> = emptyList(),
    val meetingPlace: MeetingPlace? = null,
    val isLocationSharingActive: Boolean = false,
    val error: String? = null
)

data class MemberStatus(
    val groupMemberId: Long,
    val role: String,
    val profileImageUrl: String,
    val nickname: String,
    val email: String?, // 이메일 필드 추가
    val finalAmount: Int,
    val lateFee: Int,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF2196F3),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class MeetingPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

// --- API Response ---
data class ApiEnvelope<T>(
    val message: String?,
    val data: T?
)

data class GroupGatheringRes(
    val gatheringTime: String,
    val gatheringLocation: String
)

class EmptyData
