package com.ssafy.a705.feature.chatSet

object ChatContracts {
    const val REST_BASE_URL = "https://glml.store/"
    const val WS_PATH_PRIMARY = "/ws/chat"
    const val WS_PATH_FALLBACK = "/chat"

    fun topicChat(roomId: String) = "/topic/chat.$roomId"
    fun topicRead(roomId: String) = "/topic/read-status.$roomId"
    fun topicMembers(roomId: String) = "/topic/chat/rooms/$roomId/members"

    const val SEND_MESSAGE_DEST = "/app/chat.sendMessage"
    const val READ_MESSAGE_DEST = "/app/chat.readMessage"

    const val TIME_PATTERN = "yyyy.MM.dd HH:mm:ss"
    const val TIME_ZONE = "Asia/Seoul"
}
