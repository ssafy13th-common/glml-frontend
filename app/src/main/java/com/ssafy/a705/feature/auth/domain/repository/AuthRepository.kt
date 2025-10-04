package com.ssafy.a705.feature.auth.domain.repository

interface AuthRepository {
    suspend fun logout()
    suspend fun withdrawal()
}