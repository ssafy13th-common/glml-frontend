package com.ssafy.a705.feature.board.di

import com.ssafy.a705.feature.board.data.repository.BoardRepository
import com.ssafy.a705.feature.board.data.repository.BoardRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BoardModule {

    @Binds
    @Singleton
    abstract fun bindBoardRepository(
        impl: BoardRepositoryImpl
    ): BoardRepository
}