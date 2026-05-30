package com.example.snapmind.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.snapmind.data.local.converter.StatusConverters
import com.example.snapmind.data.local.dao.ClassificationDao
import com.example.snapmind.data.local.dao.MemoDao
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.dao.MemorySearchDao
import com.example.snapmind.data.local.dao.MemoryTagDao
import com.example.snapmind.data.local.dao.OcrTextDao
import com.example.snapmind.data.local.dao.TagDao
import com.example.snapmind.data.local.dao.VisionLabelDao
import com.example.snapmind.data.local.dao.YoutubeLinkDao
import com.example.snapmind.data.local.entity.ClassificationEntity
import com.example.snapmind.data.local.entity.MemoEntity
import com.example.snapmind.data.local.entity.MemoryItemEntity
import com.example.snapmind.data.local.entity.MemorySearchFts
import com.example.snapmind.data.local.entity.MemoryTagCrossRef
import com.example.snapmind.data.local.entity.OcrTextEntity
import com.example.snapmind.data.local.entity.TagEntity
import com.example.snapmind.data.local.entity.VisionLabelEntity
import com.example.snapmind.data.local.entity.YoutubeLinkEntity

@Database(
    entities = [
        MemoryItemEntity::class,
        OcrTextEntity::class,
        ClassificationEntity::class,
        VisionLabelEntity::class,
        TagEntity::class,
        MemoryTagCrossRef::class,
        MemoEntity::class,
        YoutubeLinkEntity::class,
        MemorySearchFts::class,
    ],
    version = SnapMindDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(StatusConverters::class)
abstract class SnapMindDatabase : RoomDatabase() {

    abstract fun memoryItemDao(): MemoryItemDao
    abstract fun ocrTextDao(): OcrTextDao
    abstract fun classificationDao(): ClassificationDao
    abstract fun visionLabelDao(): VisionLabelDao
    abstract fun tagDao(): TagDao
    abstract fun memoryTagDao(): MemoryTagDao
    abstract fun memoDao(): MemoDao
    abstract fun youtubeLinkDao(): YoutubeLinkDao
    abstract fun memorySearchDao(): MemorySearchDao

    companion object {
        const val VERSION = 1
        const val NAME = "snapmind.db"
    }
}
