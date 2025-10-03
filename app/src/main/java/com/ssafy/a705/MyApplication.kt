package com.ssafy.a705

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 카카오맵 API 키 (네이티브 앱 키)
        KakaoMapSdk.init(this, "80b85b5c85a75003717f55ad076b05ea")
        KakaoSdk.init(this, "80b85b5c85a75003717f55ad076b05ea")
    }
}