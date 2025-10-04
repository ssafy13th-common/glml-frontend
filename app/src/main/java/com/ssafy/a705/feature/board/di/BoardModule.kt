package com.ssafy.a705.feature.board.di

import com.ssafy.a705.feature.board.domain.repository.BoardRepository
import com.ssafy.a705.feature.board.data.repository.BoardRepositoryImpl
import com.ssafy.a705.feature.board.data.repository.CommentRepositoryImpl
import com.ssafy.a705.feature.board.domain.repository.CommentRepository
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

    @Binds
    @Singleton
    abstract fun bindCommentRepository(
        impl: CommentRepositoryImpl
    ): CommentRepository
}