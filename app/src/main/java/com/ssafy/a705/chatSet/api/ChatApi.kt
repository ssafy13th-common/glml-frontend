package com.ssafy.a705.chatSet.api
import com.ssafy.a705.chatSet.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {
//    @GET("api/v1/chat/{roomId}/history")
//    suspend fun getHistory(@Path("roomId") roomId: String, @Query("page") page: Int, @Query("size") size: Int): ChatHistoryRes
//    @GET("api/v1/chat/rooms")
//    suspend fun getRooms(): ChatRoomsRes
//    @GET("api/v1/chat/rooms/private")
//    suspend fun getPrivateRoom(@Query("email") email: String): ChatRoomRes
//    @POST("api/v1/chat/rooms")
//    suspend fun createRoom(@Body req: CreateRoomReq): ChatRoomRes
    /**
     * 아래는 400에러의 리스폰스를 받기 위함
     */
    @GET("api/v1/chat/rooms/private")
    suspend fun getPrivateRoomResp(
        @Query("email") email: String
    ): Response<ApiEnvelope<ChatRoomRes>>

    // 방 목록: 없으면 400 (스펙)
    @GET("api/v1/chat/rooms")
    suspend fun getRoomsResp(): Response<ApiEnvelope<ChatRoomsRes>>

    // 방 생성: 200 + data=ChatRoomRes
    @POST("api/v1/chat/rooms")
    suspend fun createRoomEnvelope(
        @Body body: CreateRoomReq
    ): Response<ApiEnvelope<ChatRoomRes>>

    // 히스토리: 200 + data=ChatHistoryRes (없으면 400 가능)
    @GET("api/v1/chat/{roomId}/history")
    suspend fun getHistoryEnvelope(
        @Path("roomId") roomId: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ApiEnvelope<ChatHistoryRes>>

}
