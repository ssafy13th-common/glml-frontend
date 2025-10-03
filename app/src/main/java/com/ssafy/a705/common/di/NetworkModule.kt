package com.ssafy.a705.common.di

import android.content.Context
import com.ssafy.a705.feature.chatSet.api.ChatApi
import com.ssafy.a705.common.network.AuthInterceptor
import com.ssafy.a705.common.network.GroupApiService
import com.ssafy.a705.common.network.LiveLocationStatusApi
import com.ssafy.a705.common.network.mypage.MypageApi
import com.ssafy.a705.common.network.sign.SignApi
import com.ssafy.a705.common.network.with.WithApi
import com.ssafy.a705.feature.signup.SignupApi
import com.ssafy.a705.feature.record.map.data.remote.api.MapApi
import com.ssafy.a705.feature.record.diary.RecordApi
import com.ssafy.a705.feature.tracking.TrackingApi
import com.ssafy.a705.feature.group.latecheck.LateFeeStorage
import com.ssafy.a705.feature.group.latecheck.LiveLocationWebSocketClient
import com.ssafy.a705.feature.group.latecheck.LiveLocationRepository
import com.ssafy.a705.common.network.sign.SessionManager
import com.ssafy.a705.feature.group.chat.GroupChatApi
import com.ssafy.a705.feature.group.chat.GroupChatRepository
import com.ssafy.a705.feature.group.chat.GroupChatWebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://glml.store/"  // 백 서버 주소

    // Logger (필요시 다른 곳에서도 재사용 가능하도록 분리)
    @Provides
    @Singleton
    fun provideLogger(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    // OkHttp (공용) — Authorization 한 개 헤더는 AuthInterceptor에서 처리
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        logger: HttpLoggingInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)  // Authorization: Bearer <JWT>
            .addInterceptor(logger)
            .build()

    // Retrofit (공용) — 그룹 포함 모든 API 공통 사용
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)  // logging interceptor가 달린 클라이언트
            .addConverterFactory(GsonConverterFactory.create()) // 기본 Gson
            .build()

    // === API interfaces (모두 공용 Retrofit 사용) ===
    @Provides @Singleton fun provideSignApi(retrofit: Retrofit): SignApi =
        retrofit.create(SignApi::class.java)

    @Provides @Singleton fun provideSignupApi(retrofit: Retrofit): SignupApi =
        retrofit.create(SignupApi::class.java)

    @Provides @Singleton fun provideTrackingApi(retrofit: Retrofit): TrackingApi =
        retrofit.create(TrackingApi::class.java)

    @Provides @Singleton fun provideLocationApi(retrofit: Retrofit): MapApi =
        retrofit.create(MapApi::class.java)

    @Provides @Singleton fun provideDiaryApi(retrofit: Retrofit): RecordApi =
        retrofit.create(RecordApi::class.java)

    @Provides @Singleton fun provideMypageApi(retrofit: Retrofit): MypageApi =
        retrofit.create(MypageApi::class.java)

    @Provides @Singleton fun provideWithApi(retrofit: Retrofit): WithApi =
        retrofit.create(WithApi::class.java)

    // 그룹 API도 공용 Retrofit 사용 (별도 2중 헤더/전용 Gson 제거)
    @Provides
    @Singleton
    fun provideGroupApiService(retrofit: Retrofit): GroupApiService =
        retrofit.create(GroupApiService::class.java)

    // LiveLocation API 추가
    @Provides
    @Singleton
    fun provideLiveLocationStatusApi(retrofit: Retrofit): LiveLocationStatusApi =
        retrofit.create(LiveLocationStatusApi::class.java)

    // === etc (그룹 기능 유틸) ===
    @Provides
    @Singleton
    fun provideLateFeeStorage(@ApplicationContext context: Context): LateFeeStorage =
        LateFeeStorage(context)

    @Provides
    @Singleton
    fun provideLiveLocationWebSocketClient(
        okHttpClient: OkHttpClient
    ): LiveLocationWebSocketClient {
        return LiveLocationWebSocketClient(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideLiveLocationRepository(
        webSocketClient: LiveLocationWebSocketClient,
        sessionManager: SessionManager
    ): LiveLocationRepository =
        LiveLocationRepository(webSocketClient, sessionManager)



    @Provides @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)

    // 그룹 채팅 API 추가
    @Provides @Singleton
    fun provideGroupChatApi(retrofit: Retrofit): GroupChatApi =
        retrofit.create(GroupChatApi::class.java)

    // 그룹 채팅 WebSocket 클라이언트 추가
    @Provides
    @Singleton
    fun provideGroupChatWebSocketClient(
        okHttpClient: OkHttpClient
    ): GroupChatWebSocketClient {
        return GroupChatWebSocketClient(okHttpClient)
    }

    // 그룹 채팅 Repository 추가
    @Provides
    @Singleton
    fun provideGroupChatRepository(
        groupChatApi: GroupChatApi,
        webSocketClient: GroupChatWebSocketClient
    ): GroupChatRepository =
        GroupChatRepository(groupChatApi, webSocketClient)
}
