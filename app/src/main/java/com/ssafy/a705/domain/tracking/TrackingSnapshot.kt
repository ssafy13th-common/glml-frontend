package com.ssafy.a705.domain.tracking

data class TrackingSnapshot (
    val timestamp: Long,        // 1970-01-01 00:00:00 UTC로부터 경과된 밀리초 단위
    val latitude: Double,
    val longitude: Double
)