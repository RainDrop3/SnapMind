package com.example.snapmind.data.repository

import com.example.snapmind.data.local.dao.MemoryTagDao
import com.example.snapmind.data.local.dao.TagDao
import com.example.snapmind.data.local.entity.MemoryTagCrossRef
import com.example.snapmind.data.local.entity.TagAssignedBy
import com.example.snapmind.data.local.entity.TagAssignmentSource
import com.example.snapmind.data.local.entity.TagEntity
import javax.inject.Inject
import javax.inject.Singleton

data class TagAssignmentRequest(
    val rawName: String,
    val assignedBy: TagAssignedBy,
    val sources: Set<TagAssignmentSource>,
) {
    init {
        require(sources.isNotEmpty()) { "sources must not be empty" }
    }
}

@Singleton
class TagAssigner @Inject constructor(
    private val tagDao: TagDao,
    private val memoryTagDao: MemoryTagDao,
) {

    suspend fun assign(memoryId: Long, request: TagAssignmentRequest, now: Long): Boolean {
        val normalized = normalize(request.rawName) ?: return false
        val display = displayName(request.rawName, normalized)
        val tagId = upsertTag(normalized, display, request.assignedBy == TagAssignedBy.USER, now)

        val existing = memoryTagDao.allTagsForMemory(memoryId).firstOrNull { it.tagId == tagId }
        if (existing != null) {
            if (request.assignedBy == TagAssignedBy.AUTO && existing.removedAt != null) {
                return false
            }
            val mergedSources = mergeSources(existing.sourceTypes, request.sources)
            val updated = existing.copy(
                assignedBy = preferUserAssignment(existing.assignedBy, request.assignedBy),
                sourceTypes = mergedSources,
                removedAt = null,
            )
            memoryTagDao.upsert(updated)
            return true
        }

        memoryTagDao.upsert(
            MemoryTagCrossRef(
                memoryId = memoryId,
                tagId = tagId,
                assignedBy = request.assignedBy,
                sourceTypes = encodeSources(request.sources),
                createdAt = now,
                removedAt = null,
            ),
        )
        return true
    }

    suspend fun assignAll(memoryId: Long, requests: List<TagAssignmentRequest>, now: Long): Int =
        requests.count { assign(memoryId, it, now) }

    suspend fun removeByUser(memoryId: Long, tagId: Long, now: Long): Boolean =
        memoryTagDao.setRemovedAt(memoryId, tagId, now) > 0

    suspend fun restore(memoryId: Long, tagId: Long): Boolean =
        memoryTagDao.setRemovedAt(memoryId, tagId, null) > 0

    private suspend fun upsertTag(
        normalized: String,
        displayName: String,
        isUserManaged: Boolean,
        now: Long,
    ): Long {
        val existing = tagDao.findByName(normalized)
        if (existing != null) {
            val needsUpdate = existing.displayName != displayName ||
                (isUserManaged && !existing.isUserManaged) ||
                existing.isArchived
            if (needsUpdate) {
                tagDao.update(
                    existing.copy(
                        displayName = if (existing.displayName == normalized) displayName else existing.displayName,
                        isUserManaged = existing.isUserManaged || isUserManaged,
                        isArchived = false,
                        updatedAt = now,
                    ),
                )
            }
            return existing.id
        }
        val newRow = TagEntity(
            name = normalized,
            displayName = displayName,
            isUserManaged = isUserManaged,
            createdAt = now,
            updatedAt = now,
        )
        val inserted = tagDao.insert(newRow)
        if (inserted > 0L) return inserted
        return tagDao.findByName(normalized)?.id
            ?: error("Tag insert race for $normalized")
    }

    private fun mergeSources(existingCsv: String, additions: Set<TagAssignmentSource>): String {
        val current = decodeSources(existingCsv)
        val merged = current + additions
        return encodeSources(merged)
    }

    private fun preferUserAssignment(
        existing: TagAssignedBy,
        incoming: TagAssignedBy,
    ): TagAssignedBy =
        if (existing == TagAssignedBy.USER || incoming == TagAssignedBy.USER) {
            TagAssignedBy.USER
        } else {
            TagAssignedBy.AUTO
        }

    companion object {
        fun normalize(raw: String): String? {
            val trimmed = raw.trim().removePrefix("#").trim()
            if (trimmed.isEmpty()) return null
            return trimmed.lowercase().replace(WHITESPACE, "_")
        }

        fun displayName(raw: String, normalized: String): String {
            val cleaned = raw.trim().removePrefix("#").trim()
            return if (cleaned.isEmpty()) normalized else cleaned
        }

        fun encodeSources(sources: Set<TagAssignmentSource>): String =
            sources.toSortedSet(compareBy { it.name }).joinToString(",") { it.name }

        fun decodeSources(csv: String): Set<TagAssignmentSource> =
            csv.split(',')
                .mapNotNull { token ->
                    val key = token.trim()
                    if (key.isEmpty()) null
                    else runCatching { TagAssignmentSource.valueOf(key) }.getOrNull()
                }
                .toSet()

        private val WHITESPACE = Regex("\\s+")
    }
}
