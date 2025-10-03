package com.ssafy.a705.group.common.model

import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val messageId: String = "",
    val sender: String,
    val message: String,
    val isMine: Boolean,
    val profileUrl: String? = null,
    val timestamp: String = "",
    val readCount: Int = 0,
    val totalMembers: Int = 0, // 전체 멤버 수 추가
    val status: MessageStatus = MessageStatus.SENT,
    val tempId: String? = null
) {
    // 표시용 시간 포맷
    fun getDisplayTime(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("a h:mm", Locale.KOREAN)
            val date = inputFormat.parse(timestamp)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            timestamp
        }
    }
}

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
}
