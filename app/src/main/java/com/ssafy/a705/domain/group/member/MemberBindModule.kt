package com.ssafy.a705.domain.group.member

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MemberBindModule {
    @Binds @Singleton
    abstract fun bindGroupMemberRepository(
        impl: GroupMemberRepositoryImpl
    ): GroupMemberRepository
}