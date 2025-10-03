package com.ssafy.a705.common.di

import android.content.Context
import com.ssafy.a705.feature.tracking.TrackingPreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module     // Hilt에 의존성을 제공하는 모델
@InstallIn(SingletonComponent::class)   // 싱글톤 명시
object PreferenceModule {

    @Provides
    @Singleton
    fun provideTrackingPreferenceManager(
        @ApplicationContext context: Context
    ): TrackingPreferenceManager {
        return TrackingPreferenceManager(context)
    }
}