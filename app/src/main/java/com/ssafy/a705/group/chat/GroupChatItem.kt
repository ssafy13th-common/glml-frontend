package com.ssafy.a705.group.chat

import com.ssafy.a705.group.common.model.ChatMessage

data class GroupChatUiState(
    val groupName: String = "",
    val status: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val connectionState: WebSocketConnectionState = WebSocketConnectionState.Idle
)