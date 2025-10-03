package com.ssafy.a705.feature.chatSet.dto

data class ChatMessage(
    val id: Int,
    val senderId: Int,   // 이메일 hashCode()
    val content: String,
    val timestamp: Long
)