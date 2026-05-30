package com.example.snapmind.di

import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.data.repository.RoomMemoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMemoryRepository(
        repository: RoomMemoryRepository,
    ): MemoryRepository
}
