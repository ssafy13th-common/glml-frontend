package com.ssafy.a705.common.network.sign

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User
import com.ssafy.a705.common.network.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * 카카오 로그인/세션/토큰 관리를 담당하는 Auth Manager.
 * - 카카오 AT 획득 → 서버 교환(JWT) → 세션 저장
 * - 필요 시 AT 만료 임박하면 SDK 통해 갱신
 * - 세션 로드/정리/로그아웃
 */
@Singleton
class KakaoAuthManager @Inject constructor(
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager,
    private val signApi: SignApi,
    @ApplicationContext private val context: Context
) {

    /**
     * 카카오 로그인 → 카카오 AT 획득 → 서버 교환 → 서버 JWT 저장 → 세션 저장 후 onSuccess 콜백
     */
    fun requestKakaoToken(
        activity: Activity,
        onSuccess: (token: OAuthToken, profile: User?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val callback: (OAuthToken?, Throwable?) -> Unit = callback@ { token, error ->
            if (error != null || token == null) {
                val msg = "카카오 로그인 실패: ${friendlyMessage(error)}"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onFailure(error ?: IllegalStateException("카카오 토큰 NULL"))
                return@callback
            }

            // AT 임시 저장
            tokenManager.saveKakaoAccessToken(token.accessToken)

            // 표시용 프로필(실패 무시 가능)
            CoroutineScope(Dispatchers.IO).launch {
                val me = try {
                    suspendCancellableCoroutine<User> { cont ->
                        UserApiClient.instance.me { u, e ->
                            if (e != null) cont.resumeWithException(e) else cont.resume(u!!)
                        }
                    }
                } catch (_: Throwable) { null }

                withContext(Dispatchers.Main) { onSuccess(token, me) }
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
            UserApiClient.instance.loginWithKakaoTalk(
                context = activity,
                callback = callback
            )
        } else {
            UserApiClient.instance.loginWithKakaoAccount(
                context = activity,
                callback = callback
            )
        }
    }

    /** ② 성별/이름 입력을 받은 뒤 서버와 교환 */
    suspend fun exchangeWithServer(
        gender: SignApi.Gender,
        name: String
    ): KakaoSession {
        // 1. 카카오 SDK에서 최신 토큰 가져오기
        Log.d("KKAKAO_LOGIN", "로그인")
        val currentToken = AuthApiClient.instance.tokenManagerProvider.manager.getToken()
        val freshAccessToken = currentToken?.accessToken ?: throw IllegalStateException("카카오 토큰 없음")
        Log.d("KKAKAO_LOGIN", "$currentToken")
        // 2. 서버와 교환
        val res = signApi.loginWithKakao(
            SignApi.KakaoLoginRequest(
                accessToken = freshAccessToken,
                gender = gender,
                name = name
            )
        )
        val jwt = res.data ?: throw IllegalStateException("서버 응답에 data 없음")

        tokenManager.saveServerAccessToken(jwt.accessToken)
        tokenManager.saveServerRefreshToken(jwt.refreshToken)

        // 3. 카카오 프로필 가져오기
        val me = try {
            suspendCancellableCoroutine<User> { cont ->
                UserApiClient.instance.me { u, e ->
                    if (e != null) cont.resumeWithException(e) else cont.resume(u!!)
                }
            }
        } catch (_: Throwable) { null }

        // 4. 세션 저장
        val session = KakaoSession(
            kakaoId = me?.id ?: -1L,
            email = me?.kakaoAccount?.email,
            nickname = me?.kakaoAccount?.profile?.nickname,
            profileImageUrl = me?.kakaoAccount?.profile?.profileImageUrl,
            kakaoAccessToken = freshAccessToken,
            kakaoRefreshToken = currentToken?.refreshToken,
            kakaoExpiresAtEpochSec = currentToken?.accessTokenExpiresAt?.time?.div(1000)
        )
        sessionManager.save(session)
        return session
    }

    //login은 이름과 젠더가 없을 때 로그인하던 구버전
    fun login(
        activity: Activity,
        gender: SignApi.Gender,
        name: String,
        onSuccess: (KakaoSession) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        requestKakaoToken(
            activity = activity,
            onSuccess = { token, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val session = exchangeWithServer(

                            gender = gender,
                            name = name
                        )
                        withContext(Dispatchers.Main) { onSuccess(session) }
                    } catch (t: Throwable) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "서버 교환 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                            onFailure(t)
                        }
                    }
                }
            },
            onFailure = { t ->
                // requestKakaoToken에서 이미 상세 토스트 띄움
                onFailure(t)
            }
        )
    }
    @Deprecated("Use requestKakaoToken() + exchangeWithServer() or login(activity, gender, name, ...)")
    fun login(
        activity: Activity,
        onSuccess: (KakaoSession) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val t = IllegalStateException("gender/name 입력이 필요합니다. 새 플로우를 사용하세요.")
        Toast.makeText(context, "카카오 로그인 실패: ${t.message}", Toast.LENGTH_SHORT).show()
        onFailure(t)
    }


    /**
     * 카카오 AT 만료 임박 시 SDK로 검증/자동갱신 후 세션 업데이트.
     * - 여유가 충분하면 기존 세션 그대로 반환
     * - 실패 시 세션 정리 후 null
     */
    suspend fun refreshIfNeeded(thresholdSec: Long = 5 * 60): KakaoSession? {
        val current = sessionManager.load() ?: return null
        val expiresAt = current.kakaoExpiresAtEpochSec ?: return current
        val nowSec = System.currentTimeMillis() / 1000
        if (expiresAt - nowSec > thresholdSec) return current

        try {
            suspendCancellableCoroutine<Unit> { cont ->
                UserApiClient.instance.accessTokenInfo { _, e ->
                    if (e != null) cont.resumeWithException(e) else cont.resume(Unit)
                }
            }
        } catch (e: Throwable) {
            clearSession()
            return null
        }

        val kakaoToken = AuthApiClient.instance.tokenManagerProvider.manager.getToken()
            ?: return current

        kakaoToken.accessToken?.let { tokenManager.saveKakaoAccessToken(it) }

        val me = try {
            suspendCancellableCoroutine<User> { cont ->
                UserApiClient.instance.me { u, e ->
                    if (e != null) cont.resumeWithException(e) else cont.resume(u!!)
                }
            }
        } catch (_: Throwable) { null }

        val updated = KakaoSession(
            kakaoId = me?.id ?: current.kakaoId,
            email = me?.kakaoAccount?.email ?: current.email,
            nickname = me?.kakaoAccount?.profile?.nickname ?: current.nickname,
            profileImageUrl = me?.kakaoAccount?.profile?.profileImageUrl ?: current.profileImageUrl,
            kakaoAccessToken = kakaoToken.accessToken ?: current.kakaoAccessToken,
            kakaoRefreshToken = kakaoToken.refreshToken ?: current.kakaoRefreshToken,
            kakaoExpiresAtEpochSec = kakaoToken.accessTokenExpiresAt?.time?.div(1000)
                ?: current.kakaoExpiresAtEpochSec
        )
        sessionManager.save(updated)
        return updated
    }

    /** 현재 저장된 세션 획득 */
    fun getCurrentSession(): KakaoSession? = sessionManager.load()

    /** 세션만 정리 (선택: 토큰도 함께) */
    fun clearSession() {
        sessionManager.clear()
        tokenManager.clearServerTokens()
        tokenManager.clearKakaoTokens()
    }

    /** 앱 로그아웃 처리(필요 시 SDK 로그아웃/연결 끊기도 추가) */
    suspend fun logout() {
        // 1) Kakao SDK 로그아웃 (토큰 폐기)
        try {
            suspendCancellableCoroutine<Unit> { cont ->
                UserApiClient.instance.logout { e ->
                    if (e != null) cont.resumeWithException(e) else cont.resume(Unit)
                }
            }
        } catch (e: Throwable) {
            // 네트워크/SDK 오류가 나도 앱 쪽 정리는 계속 진행
            Log.w("KakaoLogout", "SDK logout failed, proceed local clear", e)
        }

        // 2) 앱 내부 토큰/세션 정리
        // 선택: 이 메서드들이 없다면 만들어 두세요
        tokenManager.clearServerTokens()
        tokenManager.clearKakaoTokens()
        sessionManager.clear()
    }
    private fun friendlyMessage(t: Throwable?): String {
        if (t == null) return "알 수 없는 오류"

        // KakaoSdkError든 뭐든, 문자열 기반으로만 분기 (버전 무관)
        val raw = t.toString() + " | " + (t.message ?: "")
        val lower = raw.lowercase()

        return when {
            "cancel" in lower -> "사용자가 로그인 과정을 취소했습니다"
            "network" in lower || "timeout" in lower || "failed to connect" in lower -> "네트워크 오류"
            "hash" in lower && "key" in lower -> "키해시 불일치 (카카오 콘솔 확인)"
            "not installed" in lower || "kakaotalk" in lower -> "카카오톡 미설치/호환 문제"
            "misconfig" in lower || "misconfigured" in lower -> "플랫폼 설정(패키지명/키해시) 불일치"
            "invalid token" in lower -> "유효하지 않은 토큰"
            else -> t.message ?: raw
        }
    }
    suspend fun unlinkKakao() {
        try {
            suspendCancellableCoroutine<Unit> { cont ->
                UserApiClient.instance.unlink { e ->
                    if (e != null) cont.resumeWithException(e) else cont.resume(Unit)
                }
            }
        } finally {
            // 어쨌든 로컬 정리는 수행
            clearSession()
        }
    }


}
