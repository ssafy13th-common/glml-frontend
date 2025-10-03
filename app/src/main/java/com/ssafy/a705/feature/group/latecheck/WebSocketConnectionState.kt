package com.ssafy.a705.feature.group.latecheck

sealed class WebSocketConnectionState {
    data object Idle : WebSocketConnectionState()
    data object Connecting : WebSocketConnectionState()
    data class Connected(val url: String) : WebSocketConnectionState()
    data class Closed(val code: Int, val reason: String?) : WebSocketConnectionState()
    data class Failed(val error: String?) : WebSocketConnectionState()
}
