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
import com.example.snapmind.data.local.dao.LinkPreviewDao
import com.example.snapmind.data.local.dao.ClassificationDao
import com.example.snapmind.data.local.entity.MemoEntity
import com.example.snapmind.data.local.entity.MemoryItemEntity
import com.example.snapmind.data.local.entity.TagAssignedBy
import com.example.snapmind.data.local.entity.TagAssignmentSource
import com.example.snapmind.data.local.entity.TagEntity
import com.example.snapmind.data.model.CategoryCount
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.local.entity.ClassificationEntity
import com.example.snapmind.data.local.entity.GeminiMemoStatus
import com.example.snapmind.data.local.entity.StandardProcessingStatus
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.model.TagCount
import com.example.snapmind.data.remote.common.GeminiMemoSuggester
import com.example.snapmind.data.remote.image.ImageQualityEnhancer
import com.example.snapmind.data.work.LocalMemoryProcessingWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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
    private val linkPreviewDao: LinkPreviewDao,
    private val memorySearchDao: MemorySearchDao,
    private val imageImporter: ImageImporter,
    private val tagAssigner: TagAssigner,
    private val pdfExporter: PdfExporter,
    private val geminiSuggester: GeminiMemoSuggester,
    private val imageQualityEnhancer: ImageQualityEnhancer,
    private val dispatcherProvider: DispatcherProvider,
) : MemoryRepository {

    private val scope = CoroutineScope(
        SupervisorJob() + dispatcherProvider.io +
            CoroutineExceptionHandler { _, t -> Log.e(TAG, "Unhandled exception in repository scope", t) },
    )

    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    override val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    private val _geminiInProgress = MutableStateFlow<Set<Long>>(emptySet())
    override val geminiInProgress: StateFlow<Set<Long>> = _geminiInProgress.asStateFlow()

    private val _geminiEvents = MutableSharedFlow<GeminiSuggestionEvent>(extraBufferCapacity = 4)
    override val geminiEvents: SharedFlow<GeminiSuggestionEvent> = _geminiEvents.asSharedFlow()

    private val _imageEnhancementInProgress = MutableStateFlow<Set<Long>>(emptySet())
    override val imageEnhancementInProgress: StateFlow<Set<Long>> =
        _imageEnhancementInProgress.asStateFlow()

    private val _imageEnhancementEvents = MutableSharedFlow<ImageEnhancementEvent>(extraBufferCapacity = 4)
    override val imageEnhancementEvents: SharedFlow<ImageEnhancementEvent> =
        _imageEnhancementEvents.asSharedFlow()

    /** 진행 중인 추천 작업(메모리 id별). 명시적 메모 저장 시 취소에 사용. */
    private val geminiJobs = ConcurrentHashMap<Long, Job>()

    /** 진행 중인 화질 업그레이드 작업(메모리 id별). */
    private val imageEnhancementJobs = ConcurrentHashMap<Long, Job>()

    /** 현재 상세 화면에 떠 있는 메모리 id. 추천 완료 시 칩 vs 자동 적용을 가른다. */
    @Volatile private var viewingMemoryId: Long? = null

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
                memory.categories.any { it.displayName.contains(q, ignoreCase = true) } ||
                memory.tags.any { it.contains(q, ignoreCase = true) } ||
                memory.linkPreview?.matches(q) == true
            val matchesTag = normalizedTag == null ||
                memory.tags.any { TagAssigner.normalize(it) == normalizedTag }
            val matchesCategory = category == null || category in memory.categories
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
        activeMemories().filter { category == null || category in it.categories }

    override suspend fun importImage(
        sourceUri: Uri,
        mimeType: String?,
        sourceLabel: String,
        initialMemo: String,
        initialTags: List<String>,
    ): AppResult<MemoryItem> {
        val imported = when (val r = imageImporter.import(sourceUri, mimeType)) {
            is AppResult.Success -> r.data
            is AppResult.Error -> return r
        }

        val existing = imported.contentHash?.let { memoryItemDao.findByContentHash(it) }
        if (existing != null) {
            applyImportedMetadata(existing.id, initialMemo, initialTags)
            val updated = memoryItemDao.getById(existing.id) ?: existing
            val aggregate = buildAggregate(updated)
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
                body = initialMemo.trim().ifBlank { DEFAULT_MEMO_BODY },
                geminiSuggestion = null,
                createdAt = now,
                updatedAt = now,
            ),
        )

        // 자동 태그는 분석 후 AutoTaggingWorker가 모델 top-1 결과 하나만 부여한다.
        // (가져오기 시점의 #Imported 시드 태그는 더 이상 붙이지 않는다.)
        assignUserTags(memoryId, initialTags, now)

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

    private suspend fun applyImportedMetadata(
        memoryId: Long,
        initialMemo: String,
        initialTags: List<String>,
    ) {
        val now = System.currentTimeMillis()
        var touched = false
        val memo = initialMemo.trim()
        if (memo.isNotEmpty()) {
            val existingMemo = memoDao.getByMemoryId(memoryId)
            if (existingMemo == null) {
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
            touched = true
        }
        if (assignUserTags(memoryId, initialTags, now)) {
            touched = true
        }
        if (touched) {
            memoryItemDao.touchUpdatedAt(memoryId, now)
            refreshFts(memoryId)
        }
    }

    private suspend fun assignUserTags(
        memoryId: Long,
        rawTags: List<String>,
        now: Long,
    ): Boolean {
        val tags = rawTags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { TagAssigner.normalize(it) }
        tags.forEach { tag ->
            tagAssigner.assign(
                memoryId = memoryId,
                request = TagAssignmentRequest(
                    rawName = tag,
                    assignedBy = TagAssignedBy.USER,
                    sources = setOf(TagAssignmentSource.USER),
                ),
                now = now,
            )
        }
        return tags.isNotEmpty()
    }

    override fun toggleFavorite(memoryId: Long) {
        val current = snapshot.firstOrNull { it.id == memoryId } ?: return
        val target = !current.isFavorite
        scope.launch {
            memoryItemDao.setFavorite(memoryId, target, System.currentTimeMillis())
        }
    }

    override fun updateMemo(memoryId: Long, memo: String) {
        // 사용자가 직접 쓴 메모를 명시적으로 저장하면, 진행 중인 추천이 본문을 덮어쓰지 않도록 취소한다.
        cancelGeminiJob(memoryId)
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
            memoryItemDao.touchUpdatedAt(memoryId, now)
            refreshFts(memoryId)
        }
    }

    override suspend fun addTagToMemory(memoryId: Long, tagName: String) {
        if (TagAssigner.normalize(tagName) == null) return
        val now = System.currentTimeMillis()
        tagAssigner.assign(
            memoryId = memoryId,
            request = TagAssignmentRequest(
                rawName = tagName,
                assignedBy = TagAssignedBy.USER,
                sources = setOf(TagAssignmentSource.USER),
            ),
            now = now,
        )
        memoryItemDao.touchUpdatedAt(memoryId, now)
        refreshFts(memoryId)
    }

    override suspend fun removeTagFromMemory(memoryId: Long, tagName: String) {
        val normalized = TagAssigner.normalize(tagName) ?: return
        val tagId = tagDao.findByName(normalized)?.id ?: return
        val now = System.currentTimeMillis()
        tagAssigner.removeByUser(memoryId, tagId, now)
        memoryItemDao.touchUpdatedAt(memoryId, now)
        refreshFts(memoryId)
    }

    override fun updateTags(memoryId: Long, tags: List<String>) {
        scope.launch {
            val now = System.currentTimeMillis()
            val targetNorm = tags.mapNotNull { TagAssigner.normalize(it) }.toSet()
            val currentTags = memoryTagDao.activeTagsForMemory(memoryId)
                .mapNotNull { tagDao.findById(it.tagId) }
            val currentNorm = currentTags.map { it.name }.toSet()

            var changed = false
            // 제거: 현재 있으나 목표에 없는 태그
            currentTags.forEach { tag ->
                if (tag.name !in targetNorm) {
                    tagAssigner.removeByUser(memoryId, tag.id, now)
                    changed = true
                }
            }
            // 추가: 목표에 있으나 현재 없는 태그 (원본 표시명으로 부여)
            tags.forEach { raw ->
                val norm = TagAssigner.normalize(raw) ?: return@forEach
                if (norm !in currentNorm) {
                    tagAssigner.assign(
                        memoryId = memoryId,
                        request = TagAssignmentRequest(
                            rawName = raw,
                            assignedBy = TagAssignedBy.USER,
                            sources = setOf(TagAssignmentSource.USER),
                        ),
                        now = now,
                    )
                    changed = true
                }
            }
            if (changed) {
                memoryItemDao.touchUpdatedAt(memoryId, now)
                refreshFts(memoryId)
            }
        }
    }

    override fun updateCategories(memoryId: Long, categories: List<MemoryCategory>) {
        scope.launch {
            val now = System.currentTimeMillis()
            // 사용자 지정 카테고리(최대 2개)를 rank 1·2로 저장해 모델 분류를 덮어쓴다.
            val rows = categories
                .distinct()
                .take(MemoryCategory.MAX_PER_MEMORY)
                .mapIndexed { index, category ->
                    ClassificationEntity(
                        memoryId = memoryId,
                        label = category.name.lowercase(),
                        confidence = 1f,
                        modelVersion = USER_CATEGORY_VERSION,
                        rank = index + 1,
                        createdAt = now,
                    )
                }
            classificationDao.deleteByMemoryId(memoryId)
            if (rows.isNotEmpty()) classificationDao.insertAll(rows)
            // 사용자가 직접 지정했으므로 분류 단계를 완료(SUCCESS)로 마킹해 ERROR 배지를 해소한다.
            memoryItemDao.setClassificationStatus(memoryId, StandardProcessingStatus.SUCCESS, now)
            memoryItemDao.touchUpdatedAt(memoryId, now)
            refreshFts(memoryId)
        }
    }

    override suspend fun listAllTags(): List<TagCount> {
        val counts = tagDao.allTagCounts().associate { it.name to it.count }
        return tagDao.getAllActive().map { tag ->
            TagCount(
                name = tag.name,
                displayName = "#${tag.displayName}",
                count = counts[tag.name] ?: 0,
            )
        }
    }

    override suspend fun createTag(tagName: String): Boolean {
        val normalized = TagAssigner.normalize(tagName) ?: return false
        val now = System.currentTimeMillis()
        val existing = tagDao.findByName(normalized)
        if (existing != null) {
            if (existing.isArchived) tagDao.setArchived(existing.id, false, now)
            return false
        }
        val inserted = tagDao.insert(
            TagEntity(
                name = normalized,
                displayName = TagAssigner.displayName(tagName, normalized),
                isUserManaged = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return inserted > 0L
    }

    override suspend fun deleteTag(tagName: String) {
        val normalized = TagAssigner.normalize(tagName) ?: return
        val tag = tagDao.findByName(normalized) ?: return
        val affected = memoryTagDao.memoriesForTag(tag.id).map { it.id }
        memoryTagDao.deleteByTagId(tag.id)
        tagDao.deleteById(tag.id)
        val now = System.currentTimeMillis()
        affected.forEach { id ->
            memoryItemDao.touchUpdatedAt(id, now)
            refreshFts(id)
        }
        tagSnapshot = computeTagCounts()
    }

    override fun setViewingMemory(memoryId: Long?) {
        viewingMemoryId = memoryId
    }

    override fun requestGeminiSuggestion(memoryId: Long) {
        if (memoryId <= 0L || geminiJobs.containsKey(memoryId)) return
        _geminiInProgress.update { it + memoryId }
        // start=LAZY: 맵 등록·완료 핸들러 연결 후 시작해 조기 완료 경합을 피한다.
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val result: AppResult<Unit> = when (val r = geminiSuggester.suggest(memoryId)) {
                is AppResult.Success -> {
                    val suggestion = r.data
                    val now = System.currentTimeMillis()
                    if (viewingMemoryId == memoryId) {
                        // 화면에 머무는 중: 추천 칩으로 노출하고 사용자의 수락/해제를 기다린다.
                        memoDao.updateGeminiSuggestion(memoryId, suggestion, now)
                        memoryItemDao.setGeminiMemoStatus(memoryId, GeminiMemoStatus.SUGGESTED, now)
                    } else {
                        // 화면을 나간 상태: 추천을 메모 본문에 자동 반영하고 완료 처리한다.
                        memoDao.updateBody(memoryId, suggestion, now)
                        memoDao.updateGeminiSuggestion(memoryId, null, now)
                        memoryItemDao.setGeminiMemoStatus(memoryId, GeminiMemoStatus.ACCEPTED, now)
                        memoryItemDao.touchUpdatedAt(memoryId, now)
                        refreshFts(memoryId)
                    }
                    AppResult.Success(Unit)
                }
                is AppResult.Error -> AppResult.Error(r.error)
            }
            _geminiEvents.tryEmit(GeminiSuggestionEvent(memoryId, result))
        }
        geminiJobs[memoryId] = job
        job.invokeOnCompletion {
            geminiJobs.remove(memoryId, job)
            _geminiInProgress.update { it - memoryId }
        }
        job.start()
    }

    override fun requestImageEnhancement(memoryId: Long) {
        if (memoryId <= 0L || imageEnhancementJobs.containsKey(memoryId)) return
        _imageEnhancementInProgress.update { it + memoryId }
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val result = imageQualityEnhancer.enhance(memoryId)
            _imageEnhancementEvents.tryEmit(ImageEnhancementEvent(memoryId, result))
        }
        imageEnhancementJobs[memoryId] = job
        job.invokeOnCompletion {
            imageEnhancementJobs.remove(memoryId, job)
            _imageEnhancementInProgress.update { it - memoryId }
        }
        job.start()
    }

    /** 진행 중인 추천 작업을 취소하고 RUNNING 상태가 남지 않도록 종료 상태로 마킹한다. */
    private fun cancelGeminiJob(memoryId: Long) {
        val job = geminiJobs.remove(memoryId) ?: return
        job.cancel()
        _geminiInProgress.update { it - memoryId }
        scope.launch {
            memoryItemDao.setGeminiMemoStatus(memoryId, GeminiMemoStatus.SKIPPED, System.currentTimeMillis())
        }
    }

    override fun acceptGeminiSuggestion(memoryId: Long) {
        scope.launch {
            val memo = memoDao.getByMemoryId(memoryId) ?: return@launch
            val suggestion = memo.geminiSuggestion ?: return@launch
            val now = System.currentTimeMillis()
            memoDao.updateBody(memoryId, suggestion, now)
            memoDao.updateGeminiSuggestion(memoryId, null, now)
            memoryItemDao.touchUpdatedAt(memoryId, now)
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
            classifications = classificationDao.getTopCategories(entity.id),
            linkPreview = linkPreviewDao.getByMemoryId(entity.id),
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
        private const val TAG = "RoomMemoryRepository"
        private const val DEFAULT_MEMO_BODY = "새 이미지 분석을 준비 중입니다."
        private const val USER_CATEGORY_VERSION = "user"

        @Suppress("unused")
        private val MEMORIES_STATE = SharingStarted.WhileSubscribed(5_000)
    }
}

private fun com.example.snapmind.data.model.LinkPreview.matches(query: String): Boolean =
    title?.contains(query, ignoreCase = true) == true ||
        description?.contains(query, ignoreCase = true) == true ||
        siteName?.contains(query, ignoreCase = true) == true ||
        url.contains(query, ignoreCase = true)
