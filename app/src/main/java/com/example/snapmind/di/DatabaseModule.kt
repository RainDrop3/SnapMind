package com.example.snapmind.di

import android.content.Context
import androidx.room.Room
import com.example.snapmind.data.local.SnapMindDatabase
import com.example.snapmind.data.local.dao.ClassificationDao
import com.example.snapmind.data.local.dao.MemoDao
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.dao.MemorySearchDao
import com.example.snapmind.data.local.dao.MemoryTagDao
import com.example.snapmind.data.local.dao.OcrTextDao
import com.example.snapmind.data.local.dao.TagDao
import com.example.snapmind.data.local.dao.VisionLabelDao
import com.example.snapmind.data.local.dao.YoutubeLinkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSnapMindDatabase(
        @ApplicationContext context: Context,
    ): SnapMindDatabase = Room.databaseBuilder(
        context,
        SnapMindDatabase::class.java,
        SnapMindDatabase.NAME,
    ).build()

    @Provides
    fun provideMemoryItemDao(db: SnapMindDatabase): MemoryItemDao = db.memoryItemDao()

    @Provides
    fun provideOcrTextDao(db: SnapMindDatabase): OcrTextDao = db.ocrTextDao()

    @Provides
    fun provideClassificationDao(db: SnapMindDatabase): ClassificationDao = db.classificationDao()

    @Provides
    fun provideVisionLabelDao(db: SnapMindDatabase): VisionLabelDao = db.visionLabelDao()

    @Provides
    fun provideTagDao(db: SnapMindDatabase): TagDao = db.tagDao()

    @Provides
    fun provideMemoryTagDao(db: SnapMindDatabase): MemoryTagDao = db.memoryTagDao()

    @Provides
    fun provideMemoDao(db: SnapMindDatabase): MemoDao = db.memoDao()

    @Provides
    fun provideYoutubeLinkDao(db: SnapMindDatabase): YoutubeLinkDao = db.youtubeLinkDao()

    @Provides
    fun provideMemorySearchDao(db: SnapMindDatabase): MemorySearchDao = db.memorySearchDao()
}
