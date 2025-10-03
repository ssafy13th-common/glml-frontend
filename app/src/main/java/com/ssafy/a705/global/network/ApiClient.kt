package com.ssafy.a705.global.network

import com.ssafy.a705.global.network.mypage.MypageApi
import com.ssafy.a705.global.network.sign.SignApi
import com.ssafy.a705.global.network.verification.VerificationApi
import com.ssafy.a705.global.network.with.PhoneRequirementInterceptor
import com.ssafy.a705.global.network.with.WithApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    authInterceptor: AuthInterceptor,
    phoneRequirementInterceptor: PhoneRequirementInterceptor
) {
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // 전체 로그 출력
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)//토큰
        .addInterceptor(phoneRequirementInterceptor) // 401 메시지 판별
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://glml.store/") // TODO: 환경 변수 또는 BuildConfig로 처리 권장
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val companionPostApi: WithApi = retrofit.create(WithApi::class.java)
    val verificationApi: VerificationApi = retrofit.create(VerificationApi::class.java)
    val signApi: SignApi = retrofit.create(SignApi::class.java)
    val mypageApi: MypageApi = retrofit.create(MypageApi::class.java)
}