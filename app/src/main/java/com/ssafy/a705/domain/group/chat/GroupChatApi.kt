package com.ssafy.a705.domain.group.chat

import com.ssafy.a705.model.base.BaseResponse
import retrofit2.http.*

interface GroupChatApi {
    
    // 채팅방 목록 조회
    @GET("api/v1/chat/rooms")
    suspend fun getChatRooms(): BaseResponse<ChatRoomsResponse>
    
    // 채팅 히스토리 조회
    @GET("api/v1/chat/{roomId}/history")
    suspend fun getChatHistory(
        @Path("roomId") roomId: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): BaseResponse<ChatHistoryResponse>
    
    // 읽음 상태 업데이트
    @POST("api/v1/chat/{roomId}/read-status")
    suspend fun updateReadStatus(
        @Path("roomId") roomId: String,
        @Body request: ReadStatusRequest
    ): BaseResponse<Unit>
    
    // 1:1 채팅방 생성 (필요시)
    @POST("api/v1/chat/rooms")
    suspend fun createChatRoom(
        @Body request: CreateChatRoomRequest
    ): BaseResponse<ChatRoomResponse>
}

// Request DTOs
data class ReadStatusRequest(
    val roomId: String,
    val lastReadMessageId: String
)

data class CreateChatRoomRequest(
    val name: String,
    val membersInfo: List<MemberInfo>
)

data class MemberInfo(
    val email: String,
    val nickname: String
)

// Response DTOs
data class ChatRoomsResponse(
    val rooms: List<ChatRoomInfo>
)

data class ChatRoomInfo(
    val roomId: String,
    val name: String,
    val membersInfo: List<MemberInfo> // email, nickname 배열
)

data class ChatHistoryResponse(
    val messages: List<ChatMessageDto>,
    val membersList: List<ChatMemberDto>,
    val page: Int,
    val size: Int,
    val total: Int
)

data class ChatMessageDto(
    val messageId: String,
    val roomId: String,
    val senderId: String, // email
    val content: String,
    val timestamp: String,
    val readCount: Int
)

data class ChatMemberDto(
    val email: String,
    val nickname: String
)

data class ChatRoomResponse(
    val roomId: String,
    val name: String,
    val membersIdMap: Map<String, String>
)
