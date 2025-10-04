package com.ssafy.a705.feature.auth.data.repository

import com.ssafy.a705.feature.auth.data.source.AuthRemoteDataSource
import com.ssafy.a705.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource
) : AuthRepository {
    override suspend fun logout() {
        remoteDataSource.logout()
    }

    override suspend fun withdrawal() {
        remoteDataSource.withdrawal()
    }

}