package com.ssafy.a705.common.network.base

/**
 * API 호출 실패 시 메시지를 담아 던지는 커스텀 예외
 */
class ApiException(message: String) : RuntimeException(message)