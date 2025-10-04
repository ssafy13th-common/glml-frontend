package com.ssafy.a705.feature.mypage.di

import com.ssafy.a705.feature.mypage.data.repository.MyPageRepositoryImpl
import com.ssafy.a705.feature.mypage.domain.repository.MyPageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class MyPageModule {

    @Binds
    @Singleton
    abstract fun bindMyPageRepository(
        impl: MyPageRepositoryImpl
    ): MyPageRepository
}