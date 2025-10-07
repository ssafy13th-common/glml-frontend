package com.ssafy.a705.feature.record.map.di

import com.ssafy.a705.feature.record.map.data.remote.api.MapApi
import com.ssafy.a705.feature.record.map.data.repository.MapRepositoryImpl
import com.ssafy.a705.feature.record.map.domain.repository.MapRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MapModule {
    @Binds
    @Singleton
    abstract fun bindMapRepository(impl: MapRepositoryImpl): MapRepository
}