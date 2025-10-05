package com.ssafy.a705.feature.record.diary.di

import com.ssafy.a705.feature.record.diary.data.repository.RecordRepositoryImpl
import com.ssafy.a705.feature.record.diary.domain.repository.RecordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordModule {
    @Binds
    @Singleton
    abstract fun bindRecordRepository(
        impl: RecordRepositoryImpl
    ): RecordRepository
}