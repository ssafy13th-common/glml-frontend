package com.ssafy.a705.group.common.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object GroupStatusUtil {
    
    // 서버 Enum 값들
    const val STATUS_TO_DO = "TO_DO"
    const val STATUS_IN_PROGRESS = "IN_PROGRESS"
    const val STATUS_DONE = "DONE"
    
    // UI 표시용 값들
    const val DISPLAY_TRAVEL_BEFORE = "여행 전"
    const val DISPLAY_TRAVEL_IN_PROGRESS = "여행 중"
    const val DISPLAY_TRAVEL_COMPLETED = "여행 완료"
    
    /**
     * 서버 상태값을 UI 표시용으로 변환
     */
    fun getDisplayStatus(serverStatus: String): String {
        println("🔍 getDisplayStatus 호출: serverStatus='$serverStatus'")
        val result = when (serverStatus) {
            // 서버 Enum 값들
            STATUS_TO_DO -> DISPLAY_TRAVEL_BEFORE
            STATUS_IN_PROGRESS -> DISPLAY_TRAVEL_IN_PROGRESS
            STATUS_DONE -> DISPLAY_TRAVEL_COMPLETED
            // 한글 상태값들 (서버에서 직접 한글을 반환하는 경우)
            "여행 전" -> DISPLAY_TRAVEL_BEFORE
            "여행 중" -> DISPLAY_TRAVEL_IN_PROGRESS
            "여행 완료" -> DISPLAY_TRAVEL_COMPLETED
            else -> "미정"
        }
        println("🔍 getDisplayStatus 결과: '$result'")
        return result
    }
    
    /**
     * 시작/종료 시간을 기준으로 자동 상태 업데이트
     */
    fun getAutoUpdatedStatus(
        currentStatus: String,
        startAt: String?,
        endAt: String?
    ): String {
        println("🔍 getAutoUpdatedStatus 호출: currentStatus='$currentStatus', startAt='$startAt', endAt='$endAt'")
        
        if (startAt == null || endAt == null) {
            println("🔍 날짜가 null이므로 현재 상태 유지: $currentStatus")
            return currentStatus
        }
        
        val now = LocalDateTime.now()
        println("🔍 현재 시간: $now")
        
        try {
            // 여러 날짜 형식 시도
            val startTime = parseDateTime(startAt)
            val endTime = parseDateTime(endAt)
            println("🔍 파싱된 시간: startTime=$startTime, endTime=$endTime")
            
            val result = when {
                now.isBefore(startTime) -> STATUS_TO_DO
                now.isAfter(endTime.plusDays(1)) -> STATUS_DONE
                else -> STATUS_IN_PROGRESS
            }
            println("🔍 자동 업데이트 결과: $result")
            return result
        } catch (e: Exception) {
            println("❌ 날짜 파싱 실패: startAt=$startAt, endAt=$endAt, error=${e.message}")
            // 파싱 실패 시 현재 상태 유지
            return currentStatus
        }
    }
    
    /**
     * 한글 상태값을 서버 Enum 값으로 변환
     */
    private fun convertKoreanStatusToEnum(koreanStatus: String): String {
        return when (koreanStatus) {
            "여행 전" -> STATUS_TO_DO
            "여행 중" -> STATUS_IN_PROGRESS
            "여행 완료" -> STATUS_DONE
            else -> koreanStatus // 변환할 수 없으면 원본 반환
        }
    }
    
    /**
     * 여러 날짜 형식을 지원하는 파싱 함수
     */
    private fun parseDateTime(dateTimeStr: String): LocalDateTime {
        return try {
            // 1. yyyy-MM-dd 형식 시도
            LocalDateTime.parse(dateTimeStr + "T00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        } catch (e: Exception) {
            try {
                // 2. yyyy-MM-dd'T'HH:mm:ss.SSS'Z' 형식 시도
                LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
            } catch (e2: Exception) {
                try {
                    // 3. yyyy-MM-dd'T'HH:mm:ss 형식 시도
                    LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                } catch (e3: Exception) {
                    // 4. ISO_INSTANT 형식 시도
                    val instant = java.time.Instant.parse(dateTimeStr)
                    instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                }
            }
        }
    }
}
