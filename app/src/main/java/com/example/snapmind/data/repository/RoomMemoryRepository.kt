package com.example.snapmind.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.snapmind.core.coroutine.DispatcherProvider
import com.example.snapmind.core.image.ImageImporter
import com.example.snapmind.core.pdf.PdfExporter
import com.example.snapmind.core.result.AppError
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.local.dao.MemoDao
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.dao.MemorySearchDao
import com.example.snapmind.data.local.dao.MemoryTagDao
import com.example.snapmind.data.local.dao.OcrTextDao
import com.example.snapmind.data.local.dao.TagDao
import com.example.snapmind.data.local.dao.YoutubeLinkDao
import com.example.snapmind.data.local.dao.ClassificationDao
import com.example.snapmind.data.local.entity.MemoEntity
import com.example.snapmind.data.local.entity.MemoryItemEntity
import com.example.snapmind.data.local.entity.TagAssignedBy
import com.example.snapmind.data.local.entity.TagAssignmentSource
import com.example.snapmind.data.model.CategoryCount
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.model.TagCount
import com.example.snapmind.data.work.LocalMemoryProcessingWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Singleton
class RoomMemoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryItemDao: MemoryItemDao,
    private val ocrTextDao: OcrTextDao,
    private val memoDao: MemoDao,
    private val tagDao: TagDao,
    private val memoryTagDao: MemoryTagDao,
    private val classificationDao: ClassificationDao,
    private val youtubeLinkDao: YoutubeLinkDao,
    private val memorySearchDao: MemorySearchDao,
    private val imageImporter: ImageImporter,
    private val tagAssigner: TagAssigner,
    private val pdfExporter: PdfExporter,
    private val dispatcherProvider: DispatcherProvider,
) : MemoryRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    override val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    @Volatile private var snapshot: List<MemoryItem> = emptyList()
    @Volatile private var categorySnapshot: List<CategoryCount> = emptyList()
    @Volatile private var tagSnapshot: List<TagCount> = emptyList()

    init {
        combine(
            memoryItemDao.observeActive(),
            memoryItemDao.observeTrashed(),
        ) { active, trashed -> active + trashed }
            .onEach { entities ->
                val aggregates = entities.map { buildAggregate(it) }
                val domain = aggregates.map { it.toDomain() }
                snapshot = domain
                _memories.value = domain
                categorySnapshot = computeCategoryCounts()
                tagSnapshot = computeTagCounts()
            }
            .launchIn(scope)
    }

    override fun getMemory(memoryId: Long): MemoryItem? =
        snapshot.firstOrNull { it.id == memoryId }

    override fun activeMemories(): List<MemoryItem> =
        snapshot.filterNot { it.isDeleted }.sortedByDescending { it.createdAtMillis }

    override fun favoriteMemories(): List<MemoryItem> =
        activeMemories().filter { it.isFavorite }

    override fun trashedMemories(): List<MemoryItem> =
        snapshot.filter { it.isDeleted }.sortedByDescending { it.deletedAtMillis ?: 0L }

    override fun topTags(limit: Int): List<TagCount> = tagSnapshot.take(limit)

    override fun categoryCounts(): List<CategoryCount> = categorySnapshot

    override fun tags(): List<TagCount> = tagSnapshot

    override fun searchMemories(
        query: String,
        tagName: String?,
        category: MemoryCategory?,
    ): List<MemoryItem> {
        val q = query.trim()
        val normalizedTag = tagName?.let { TagAssigner.normalize(it) }
        return activeMemories().filter { memory ->
            val matchesQuery = q.isBlank() ||
                memory.memo.contains(q, ignoreCase = true) ||
                memory.ocrText.contains(q, ignoreCase = true) ||
                memory.category.displayName.contains(q, ignoreCase = true) ||
                memory.tags.any { it.contains(q, ignoreCase = true) } ||
                memory.youtubeTitle?.contains(q, ignoreCase = true) == true
            val matchesTag = normalizedTag == null ||
                memory.tags.any { TagAssigner.normalize(it) == normalizedTag }
            val matchesCategory = category == null || memory.category == category
            matchesQuery && matchesTag && matchesCategory
        }
    }

    override fun filterByTag(tagName: String?): List<MemoryItem> {
        val normalized = tagName?.let { TagAssigner.normalize(it) }
        return activeMemories().filter { memory ->
            normalized == null || memory.tags.any { TagAssigner.normalize(it) == normalized }
        }
    }

    override fun filterByCategory(category: MemoryCategory?): List<MemoryItem> =
        activeMemories().filter { category == null || it.category == category }

    override suspend fun importImage(
        sourceUri: Uri,
        mimeType: String?,
        sourceLabel: String,
    ): AppResult<MemoryItem> {
        val imported = when (val r = imageImporter.import(sourceUri, mimeType)) {
            is AppResult.Success -> r.data
            is AppResult.Error -> return r
        }

        val existing = imported.contentHash?.let { memoryItemDao.findByContentHash(it) }
        if (existing != null) {
            val aggregate = buildAggregate(existing)
            return AppResult.Success(aggregate.toDomain())
        }

        val now = System.currentTimeMillis()
        val entity = MemoryItemEntity(
            imageUri = imported.targetUri,
            sourceUri = imported.sourceUri,
            mimeType = imported.mimeType,
            contentHash = imported.contentHash,
            createdAt = now,
            updatedAt = now,
        )
        val memoryId = memoryItemDao.insert(entity)
        if (memoryId <= 0L) {
            return AppResult.Error(AppError.Unknown("memory insert failed"))
        }

        memoDao.upsert(
            MemoEntity(
                memoryId = memoryId,
                body = DEFAULT_MEMO_BODY,
                geminiSuggestion = null,
                createdAt = now,
                updatedAt = now,
            ),
        )

        tagAssigner.assign(
            memoryId = memoryId,
            request = TagAssignmentRequest(
                rawName = SEED_IMPORT_TAG,
                assignedBy = TagAssignedBy.AUTO,
                sources = setOf(TagAssignmentSource.SYSTEM),
            ),
            now = now,
        )

        refreshFts(memoryId)
        enqueueLocalProcessing(memoryId)

        val stored = memoryItemDao.getById(memoryId)
            ?: return AppResult.Error(AppError.Unknown("memory missing after insert"))
        return AppResult.Success(buildAggregate(stored).toDomain())
    }

    private fun enqueueLocalProcessing(memoryId: Long) {
        val request = OneTimeWorkRequestBuilder<LocalMemoryProcessingWorker>()
            .setInputData(workDataOf(LocalMemoryProcessingWorker.KEY_MEMORY_ID to memoryId))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    override fun toggleFavorite(memoryId: Long) {
        val current = snapshot.firstOrNull { it.id == memoryId } ?: return
        val target = !current.isFavorite
        scope.launch {
            memoryItemDao.setFavorite(memoryId, target, System.currentTimeMillis())
        }
    }

    override fun updateMemo(memoryId: Long, memo: String) {
        scope.launch {
            val now = System.currentTimeMillis()
            val existing = memoDao.getByMemoryId(memoryId)
            if (existing == null) {
                memoDao.upsert(
                    MemoEntity(
                        memoryId = memoryId,
                        body = memo,
                        geminiSuggestion = null,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                memoDao.updateBody(memoryId, memo, now)
            }
            refreshFts(memoryId)
        }
    }

    override fun acceptGeminiSuggestion(memoryId: Long) {
        scope.launch {
            val memo = memoDao.getByMemoryId(memoryId) ?: return@launch
            val suggestion = memo.geminiSuggestion ?: return@launch
            val now = System.currentTimeMillis()
            memoDao.updateBody(memoryId, suggestion, now)
            memoDao.updateGeminiSuggestion(memoryId, null, now)
            refreshFts(memoryId)
        }
    }

    override fun dismissGeminiSuggestion(memoryId: Long) {
        scope.launch {
            memoDao.updateGeminiSuggestion(memoryId, null, System.currentTimeMillis())
        }
    }

    override fun softDelete(memoryId: Long) {
        scope.launch {
            val now = System.currentTimeMillis()
            memoryItemDao.setDeletedAt(memoryId, now, now)
        }
    }

    override fun restore(memoryId: Long) {
        scope.launch {
            memoryItemDao.setDeletedAt(memoryId, null, System.currentTimeMillis())
        }
    }

    override suspend fun permanentDelete(memoryId: Long): AppResult<Unit> {
        val entity = memoryItemDao.getById(memoryId)
            ?: return AppResult.Success(Unit)
        return runCatching {
            memorySearchDao.deleteIndex(memoryId)
            memoryItemDao.deleteById(memoryId)
            entity.imageUri.takeIf { it.startsWith("file://") }?.let { uriString ->
                val path = uriString.removePrefix("file://")
                val file = java.io.File(java.net.URLDecoder.decode(path, Charsets.UTF_8.name()))
                if (file.exists() && file.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
                    file.delete()
                }
            }
            AppResult.Success(Unit) as AppResult<Unit>
        }.getOrElse { AppResult.Error(AppError.Unknown(it.message.orEmpty())) }
    }

    override suspend fun searchFts(query: String): List<MemoryItem> {
        val active = activeMemories()
        val sanitized = query.trim()
        if (sanitized.isEmpty()) return active
        val tokens = escapeFtsQuery(sanitized)
        if (tokens.isEmpty()) return active
        val matchingIds = runCatching { memorySearchDao.searchIds(tokens) }.getOrDefault(emptyList())
        if (matchingIds.isEmpty()) return emptyList()
        val idSet = matchingIds.toSet()
        return active.filter { it.id in idSet }
    }

    override suspend fun exportToPdf(memoryIds: List<Long>): AppResult<android.net.Uri> {
        val pool = if (memoryIds.isEmpty()) activeMemories() else {
            val idSet = memoryIds.toSet()
            snapshot.filter { it.id in idSet }
        }
        return pdfExporter.export(pool)
    }

    private fun escapeFtsQuery(raw: String): String =
        raw.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                val cleaned = token.replace("\"", "")
                "\"$cleaned\"*"
            }

    private suspend fun buildAggregate(entity: MemoryItemEntity): MemoryAggregate {
        val activeRefs = memoryTagDao.activeTagsForMemory(entity.id)
        val tagEntities = activeRefs.mapNotNull { tagDao.findById(it.tagId) }
        return MemoryAggregate(
            item = entity,
            ocr = ocrTextDao.getByMemoryId(entity.id),
            memo = memoDao.getByMemoryId(entity.id),
            tags = tagEntities,
            topClassification = classificationDao.getTopByMemoryId(entity.id),
            youtubeLink = youtubeLinkDao.getByMemoryId(entity.id),
        )
    }

    private suspend fun refreshFts(memoryId: Long) {
        val entity = memoryItemDao.getById(memoryId) ?: return
        val aggregate = buildAggregate(entity)
        memorySearchDao.upsertIndex(buildFtsRow(aggregate))
    }

    private suspend fun computeCategoryCounts(): List<CategoryCount> {
        val rows = classificationDao.categoryCounts()
        if (rows.isEmpty()) return emptyList()
        val grouped = rows
            .groupBy { it.label.toMemoryCategory() }
            .mapValues { (_, list) -> list.sumOf { it.count } }
        return grouped.entries
            .sortedWith(
                compareByDescending<Map.Entry<MemoryCategory, Int>> { it.value }
                    .thenBy { it.key.displayName }
            )
            .map { (category, count) -> CategoryCount(category = category, count = count) }
    }

    private suspend fun computeTagCounts(): List<TagCount> {
        val rows = tagDao.allTagCounts()
        return rows.map { TagCount(name = it.name, displayName = "#${it.displayName}", count = it.count) }
    }

    companion object {
        private const val DEFAULT_MEMO_BODY = "새 이미지 분석을 준비 중입니다."
        private const val SEED_IMPORT_TAG = "Imported"

        @Suppress("unused")
        private val MEMORIES_STATE = SharingStarted.WhileSubscribed(5_000)
    }
}
