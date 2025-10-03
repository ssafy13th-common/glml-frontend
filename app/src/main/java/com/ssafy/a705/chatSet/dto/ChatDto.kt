// ChatDtos.kt
package com.ssafy.a705.chatSet.dto

import com.google.gson.annotations.SerializedName

data class ChatMemberInfoDTO(
    @SerializedName("email") val email: String,
    @SerializedName("nickname") val nickname: String
)

data class ChatRoomRes(
    val roomId: String,
    val name: String,
    @SerializedName("membersInfo") val membersInfo: List<ChatMemberInfoDTO>? = null,
    @SerializedName("membersIdMap") val membersIdMap: List<ChatMemberInfoDTO>? = null
) {
    fun participants(): List<ChatMemberInfoDTO> =
        when {
            !membersInfo.isNullOrEmpty() -> membersInfo
            !membersIdMap.isNullOrEmpty() -> membersIdMap
            else -> emptyList()
        }
}

data class ChatRoomsRes(
    val rooms: List<ChatRoomRes> = emptyList()
)

data class CreateRoomReq(
    val name: String = "",
    val membersInfo: List<ChatMemberInfoDTO> = emptyList()
)

data class ChatMessageDTO(
    @SerializedName("messageId") val messageId: String,
    @SerializedName("roomId") val roomId: String,
    @SerializedName("senderId") val senderId: String,   // 이메일
    @SerializedName("content") val content: String,
    @SerializedName("timestamp") val timestamp: String, // "2025.08.14 02:17:02"
    @SerializedName("readCount") val readCount: Int,
    @SerializedName("status") val status: AckStatus? = null

)

data class ChatHistoryRes(
    val messages: List<ChatMessageDTO> = emptyList(),
    val membersList: List<ChatMemberInfoDTO> = emptyList(),
    val page: Int = 0,
    val size: Int = 30,
    val total: Int = 0
)
@kotlinx.serialization.Serializable
data class ChatMessageEvent(
    val tempId: String? = null,
    val roomId: String,
    val messageId: String,
    val senderId: String,
    val content: String,
    val timestamp: String,
    val status: AckStatus?
)

enum class AckStatus {
    DELIVERED,  // 서버로 메시지가 도착
    SENT        // 서버에서 클라이언트로 전달됨
}


data class ReadStatusUpdateRes(
    val roomId: String,
    val messageId: String,
    val readCount: Int
)
data class SendMessageReq(
    val tempId: String,
    val roomId: String,
    val content: String
)

/**
 * 읽음 상태 갱신 요청
 * - 서버 스펙: roomId, lastReadMessageId
 */
data class ReadLogUpdateReq(
    val roomId: String,
    val lastReadMessageId: String
)
data class UiMessage(
    val id: String,
    val content: String,
    val timestamp: String,
    val isMine: Boolean,
    val readCount: Int,
    val senderName: String   // 화면에 보일 이름(닉네임)
)