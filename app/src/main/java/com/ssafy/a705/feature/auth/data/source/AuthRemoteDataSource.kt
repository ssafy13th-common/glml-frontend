package com.ssafy.a705.feature.auth.data.source

import javax.inject.Inject

class AuthRemoteDataSource @Inject constructor(private val api: AuthApi) {
    suspend fun logout() = api.logout()
    suspend fun withdrawal() = api.withdrawal()
}