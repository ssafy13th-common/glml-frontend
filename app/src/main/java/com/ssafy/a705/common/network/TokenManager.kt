package com.ssafy.a705.common.network

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    // --- Kakao SDK Access Token ---
    fun saveKakaoAccessToken(token: String) {
        prefs.edit().putString("kakaoAT", token).apply()
    }
    fun getKakaoAccessToken(): String? = prefs.getString("kakaoAT", null)
    fun clearKakaoTokens() {
        prefs.edit().remove("kakaoAT").apply()
    }

    // --- Server JWT ---
    fun saveServerAccessToken(token: String) {
        prefs.edit().putString("serverAT", token).apply()
    }

    // JWT 획득
    fun getServerAccessToken(): String? = prefs.getString("serverAT", null)

    fun saveServerRefreshToken(token: String) {
        prefs.edit().putString("serverRT", token).apply()
    }
    fun getServerRefreshToken(): String? = prefs.getString("serverRT", null)

    fun clearServerTokens() {
        prefs.edit()
            .remove("serverAT")
            .remove("serverRT")
            .apply()
    }

    // 전체 초기화가 필요할 때만 사용
    fun clearAll() {
        prefs.edit().clear().apply()
    }

}