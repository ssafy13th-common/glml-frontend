package com.ssafy.a705.feature.group.memo

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GroupMemoBindModule {

    @Binds
    @Singleton
    abstract fun bindGroupMemoRepository(
        impl: GroupMemoRepositoryImpl
    ): GroupMemoRepository
}
