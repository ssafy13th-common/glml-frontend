package com.ssafy.a705.global.network

import android.util.Log
import com.ssafy.a705.global.event.AuthEvent
import com.ssafy.a705.global.event.AuthEventBus
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val orig = chain.request()
        val sb = StringBuilder()

        // 1) Authorization 헤더 주입 (중복 방지)
        val serverJwt = tokenManager.getServerAccessToken()
        val hasAuthHeader = orig.header("Authorization")?.isNotBlank() == true
        val reqBuilder = orig.newBuilder()
        if (!hasAuthHeader && !serverJwt.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $serverJwt")
        }
        val req = reqBuilder.build()

        // 간단 요청 로그
        sb.append("➡️ ${req.method} ${req.url}")
        req.body?.let {
            try {
                val copy = req.newBuilder().build()
                val buffer = Buffer()
                it.writeTo(buffer)
                val bodyPreview = buffer.readUtf8().take(512)
                if (bodyPreview.isNotBlank()) sb.append("\nREQ_BODY: $bodyPreview")
            } catch (_: Throwable) {}
        }

        val res: Response = try {
            chain.proceed(req)
        } catch (e: Throwable) {
            // 네트워크 예외 → 화면에서 빈 화면만 보이지 않게 명확한 로그 남김
            Log.e("AuthInterceptor", " Network error: ${req.method} ${req.url}", e)
            throw e
        }

        // 2) 응답 로깅 (peekBody로 원본 소모하지 않음)
        sb.append("\n⬅️ ${res.code} ${res.message}")
        val peek: ResponseBody? = try { res.peekBody(1024 * 4) } catch (_: Throwable) { null }
        val peekStr = peek?.string()?.take(1024)
        if (!peekStr.isNullOrBlank()) sb.append("\nRES_BODY: $peekStr")
        Log.d("AuthInterceptor", sb.toString())

        // 3) 인증 관련 상태 코드 핸들링 (이벤트 발행 → NavGraph가 받아 화면전환)
        when (res.code) {
            401 -> if (!isAuthPath(req.url.encodedPath)) {
                AuthEventBus.emit(AuthEvent.RequireLogin(nextRoute = null))
            }
        }


        return res
    }

}
private fun isAuthPath(path: String): Boolean {
    return path.startsWith("/api/v1/auth") ||
            path.startsWith("/api/v1/members/phone") ||
            path.startsWith("/api/v1/phone") ||
            path.startsWith("/api/v1/auth/kakao/mobile")
}

