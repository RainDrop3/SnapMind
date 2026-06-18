package com.example.snapmind.data.repository

import com.example.snapmind.data.local.dao.ClassificationDao
import com.example.snapmind.data.local.dao.MemoDao
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.dao.MemorySearchDao
import com.example.snapmind.data.local.dao.MemoryTagDao
import com.example.snapmind.data.local.dao.OcrTextDao
import com.example.snapmind.data.local.dao.TagDao
import com.example.snapmind.data.local.dao.YoutubeLinkDao
import com.example.snapmind.data.local.entity.MemoryItemEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryAggregateBuilder @Inject constructor(
    private val ocrTextDao: OcrTextDao,
    private val memoDao: MemoDao,
    private val tagDao: TagDao,
    private val memoryTagDao: MemoryTagDao,
    private val classificationDao: ClassificationDao,
    private val youtubeLinkDao: YoutubeLinkDao,
) {

    suspend fun build(entity: MemoryItemEntity): MemoryAggregate {
        val activeRefs = memoryTagDao.activeTagsForMemory(entity.id)
        val tagEntities = activeRefs.mapNotNull { tagDao.findById(it.tagId) }
        return MemoryAggregate(
            item = entity,
            ocr = ocrTextDao.getByMemoryId(entity.id),
            memo = memoDao.getByMemoryId(entity.id),
            tags = tagEntities,
            classifications = classificationDao.getTopCategories(entity.id),
            youtubeLink = youtubeLinkDao.getByMemoryId(entity.id),
        )
    }
}

suspend fun refreshFtsRow(
    memoryId: Long,
    memoryItemDao: MemoryItemDao,
    aggregateBuilder: MemoryAggregateBuilder,
    memorySearchDao: MemorySearchDao,
) {
    val entity = memoryItemDao.getById(memoryId) ?: return
    val aggregate = aggregateBuilder.build(entity)
    memorySearchDao.upsertIndex(buildFtsRow(aggregate))
}
