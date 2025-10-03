package com.ssafy.a705.domain.with.model

data class ChatMessage(
    val id: Int,
    val senderId: Int,   // 이메일 hashCode()
    val content: String,
    val timestamp: Long
)