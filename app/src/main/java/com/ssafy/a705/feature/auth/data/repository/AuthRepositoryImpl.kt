package com.ssafy.a705.feature.auth.data.repository

import com.ssafy.a705.common.network.TokenManager
import com.ssafy.a705.common.network.base.ApiException
import com.ssafy.a705.feature.auth.data.model.request.KakaoLoginRequest
import com.ssafy.a705.feature.auth.data.model.request.LoginRequest
import com.ssafy.a705.feature.auth.data.model.response.JwtResponse
import com.ssafy.a705.feature.auth.data.source.AuthRemoteDataSource
import com.ssafy.a705.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val tokenManager: TokenManager
) : AuthRepository {
    override suspend fun login(request: LoginRequest): JwtResponse {
        val res = remoteDataSource.login(request)
        res.message?.let { throw ApiException(it) }

        val data = res.data ?: throw ApiException("로그인 실패")

        // 토큰 저장
        val atClean = data.accessToken.removePrefix("Bearer ").trim()
        tokenManager.saveServerAccessToken(atClean)

        data.refreshToken.let { rt ->
            val rtClean = rt.removePrefix("Bearer ").trim()
            tokenManager.saveServerRefreshToken(rtClean)
        }

        return data
    }

    override suspend fun logout() {
        remoteDataSource.logout()
    }

    override suspend fun loginWithKakao(request: KakaoLoginRequest): JwtResponse {
        val res = remoteDataSource.loginWithKakao(request)
        res.message?.let { throw ApiException(it) }
        return res.data ?: throw ApiException("카카오 로그인 실패")
    }
}