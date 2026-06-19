package com.example.snapmind.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.snapmind.data.local.SnapMindDatabase
import com.example.snapmind.data.local.dao.ClassificationDao
import com.example.snapmind.data.local.dao.LinkPreviewDao
import com.example.snapmind.data.local.dao.MemoDao
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.dao.MemorySearchDao
import com.example.snapmind.data.local.dao.MemoryTagDao
import com.example.snapmind.data.local.dao.OcrTextDao
import com.example.snapmind.data.local.dao.TagDao
import com.example.snapmind.data.local.dao.VisionLabelDao
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
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

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
    fun provideLinkPreviewDao(db: SnapMindDatabase): LinkPreviewDao = db.linkPreviewDao()

    @Provides
    fun provideMemorySearchDao(db: SnapMindDatabase): MemorySearchDao = db.memorySearchDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE youtube_links ADD COLUMN description TEXT")
            db.execSQL("ALTER TABLE youtube_links ADD COLUMN imageUrl TEXT")
            db.execSQL("ALTER TABLE youtube_links ADD COLUMN siteName TEXT")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE youtube_links ADD COLUMN safetyStatus TEXT NOT NULL DEFAULT 'UNCHECKED'")
            db.execSQL("ALTER TABLE youtube_links ADD COLUMN safetyThreatTypes TEXT")
            db.execSQL("ALTER TABLE youtube_links ADD COLUMN safetyCheckedAt INTEGER")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE memory_items ADD COLUMN originalImageUri TEXT")
            db.execSQL("ALTER TABLE memory_items ADD COLUMN imageEnhancementStatus TEXT NOT NULL DEFAULT 'IDLE'")
            db.execSQL("ALTER TABLE memory_items ADD COLUMN imageEnhancementProvider TEXT")
            db.execSQL("ALTER TABLE memory_items ADD COLUMN imageEnhancedAt INTEGER")
        }
    }
}
