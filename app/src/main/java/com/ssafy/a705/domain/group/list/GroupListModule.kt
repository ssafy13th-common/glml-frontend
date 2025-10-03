package com.ssafy.a705.domain.group.list

import com.ssafy.a705.group.list.GroupRepository
import com.ssafy.a705.group.list.GroupRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GroupListModule {
    
    @Binds
    @Singleton
    abstract fun bindGroupRepository(
        groupRepositoryImpl: GroupRepositoryImpl
    ): GroupRepository
}
