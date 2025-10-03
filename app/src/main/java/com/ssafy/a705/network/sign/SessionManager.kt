package com.ssafy.a705.network.sign

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("kakao_session_prefs", Context.MODE_PRIVATE)

    fun save(session: KakaoSession) {
        prefs.edit().apply {
            putLong("kakaoId", session.kakaoId)
            putString("email", session.email)
            putString("nickname", session.nickname)
            putString("profileImageUrl", session.profileImageUrl)
            putString("kakaoAT", session.kakaoAccessToken)
            putString("kakaoRT", session.kakaoRefreshToken)
            session.kakaoExpiresAtEpochSec?.let { putLong("kakaoExp", it) }
        }.apply()
    }

    fun load(): KakaoSession? {
        val id = prefs.getLong("kakaoId", -1L)
        val at = prefs.getString("kakaoAT", null)
        if (id == -1L || at.isNullOrBlank()) return null
        return KakaoSession(
            kakaoId = id,
            email = prefs.getString("email", null),
            nickname = prefs.getString("nickname", null),
            profileImageUrl = prefs.getString("profileImageUrl", null),
            kakaoAccessToken = at,
            kakaoRefreshToken = prefs.getString("kakaoRT", null),
            kakaoExpiresAtEpochSec = if (prefs.contains("kakaoExp")) prefs.getLong("kakaoExp", 0L) else null
        )
    }

    fun clear() = prefs.edit().clear().apply()
}