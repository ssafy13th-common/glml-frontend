package com.ssafy.a705.feature.controller.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a705.feature.chatSet.api.ChatApi
import com.ssafy.a705.feature.mypage.ui.screen.ChatItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MyMessagesViewModel @Inject constructor(
    private val api: ChatApi
) : ViewModel() {

    var items by mutableStateOf<List<ChatItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun load(myEmail: String) {
        if (loading) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val rooms = api.getRoomsResp().let { resp ->
                    when {
                        resp.isSuccessful -> resp.body()?.data?.rooms ?: emptyList()
                        resp.code() == 400 -> emptyList()
                        else -> error("rooms HTTP ${resp.code()}")
                    }
                }

                val enriched = rooms.map { room ->
                    // 상대 닉네임 계산 (1:1 가정, 그룹채팅이면 fallback)
                    val peers = room.participants().filter { it.email != myEmail }
                    val displayName = peers.firstOrNull()?.nickname
                        ?: room.name // 참가자 계산 실패 시 기존 방 이름

                    // 마지막 메시지(최신 1개) 가져오기
                    val last = try {
                        api.getHistoryEnvelope(room.roomId, page = 0, size = 1)
                            .body()?.data?.messages?.firstOrNull()
                    } catch (e: HttpException) {
                        if (e.code() == 400) null else throw e
                    }

                    ChatItem(
                        roomId = room.roomId,
                        name = displayName,
                        lastMessage = last?.content ?: "",
                        lastDate = last?.timestamp?.let(::parseServerTime) ?: LocalDateTime.now()
                    )
                }.sortedByDescending { it.lastDate }
                items = enriched
            } catch (t: Throwable) {
                error = t.message
            } finally {
                loading = false
            }
        }
    }
}
private fun parseServerTime(s: String): LocalDateTime {
    val fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
    return LocalDateTime.parse(s, fmt)
}