package com.ssafy.a705.common.network.with

import com.google.gson.Gson
import com.ssafy.a705.common.event.AuthEvent
import com.ssafy.a705.common.event.AuthEventBus
import com.ssafy.a705.common.network.base.ApiError
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class PhoneRequirementInterceptor @Inject constructor() : Interceptor {

    // Hilt 주입 대신 내부에서 직접 생성
    private val gson by lazy { Gson() }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val resp = chain.proceed(req)

        if (resp.code == 401) {
            val hasBearer = (req.header("Authorization") ?: "").startsWith("Bearer ")
            val hasAccess = !req.header("AccessToken").isNullOrBlank()
            val hadToken = hasBearer || hasAccess

            val nextRoute = req.header("X-Next-Route") ?: req.url.encodedPath

            if (hadToken) {
                val bodyStr = resp.peekBody(8 * 1024).string()
                val apiErr = runCatching { gson.fromJson(bodyStr, ApiError::class.java) }.getOrNull()
                val reason = apiErr?.message
                AuthEventBus.emit(AuthEvent.RequirePhoneVerification(nextRoute, reason))
            } else {
                AuthEventBus.emit(AuthEvent.RequireLogin(nextRoute))
            }
        }
        return resp
    }
}