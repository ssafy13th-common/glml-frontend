package com.ssafy.a705.common

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk
import com.ssafy.a705.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 카카오맵 API 키 (네이티브 앱 키)
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}