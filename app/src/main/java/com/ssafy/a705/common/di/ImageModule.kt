package com.ssafy.a705.common.di

import com.ssafy.a705.common.imageS3.ImageApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {
    @Provides
    @Singleton
    fun provideImageApi(retrofit: Retrofit): ImageApi =
        retrofit.create(ImageApi::class.java)
}