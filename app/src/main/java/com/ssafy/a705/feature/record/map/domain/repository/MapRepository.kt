package com.ssafy.a705.feature.record.map.domain.repository

interface MapRepository {
    suspend fun getMapColors(): Map<String, String>
    suspend fun updateMapColor(code: String, rawColor: String)
}
