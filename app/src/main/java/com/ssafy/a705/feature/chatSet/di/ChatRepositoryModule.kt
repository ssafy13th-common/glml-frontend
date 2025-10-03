package com.ssafy.a705.feature.chatSet.di

import com.ssafy.a705.feature.chatSet.repo.ChatRepository
import com.ssafy.a705.feature.chatSet.repo.ChatRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository
}
