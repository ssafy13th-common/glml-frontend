package com.ssafy.a705.common.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AuthEventBus {
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun emit(event: AuthEvent) {
        _events.tryEmit(event)
    }
}

sealed class AuthEvent {
    data class RequireLogin(val nextRoute: String?) : AuthEvent()
    data class RequirePhoneVerification(
        val nextRoute: String?,
        val reason: String? = null   // 👈 기본값
    ) : AuthEvent()
}