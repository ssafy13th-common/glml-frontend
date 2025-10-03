package com.ssafy.a705.feature.chatSet.repo
import com.ssafy.a705.feature.chatSet.dto.ChatHistoryRes
import com.ssafy.a705.feature.chatSet.dto.ReadLogUpdateReq
import com.ssafy.a705.feature.chatSet.dto.SendMessageReq
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun loadHistory(roomId: String, page: Int, size: Int): ChatHistoryRes
    fun connectAndSubscribe(wsPrimary: String, wsFallback: String, roomId: String): Flow<Any>
    fun sendMessage(req: SendMessageReq)
    fun sendRead(req: ReadLogUpdateReq)
    fun disconnect()
}
