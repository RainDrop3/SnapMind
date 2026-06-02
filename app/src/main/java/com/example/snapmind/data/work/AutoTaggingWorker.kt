package com.example.snapmind.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.dao.MemorySearchDao
import com.example.snapmind.data.local.entity.StandardProcessingStatus
import com.example.snapmind.data.repository.MemoryAggregateBuilder
import com.example.snapmind.data.repository.refreshFtsRow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 업로드 시 자동 태깅은 제거되었다.
 * - 카테고리: 모델 분류 결과(classifications, top-1)로 결정된다.
 * - 태그: 사용자가 상세 화면/태그 관리에서 직접 추가한 것만 사용한다.
 *
 * 이 워커는 더 이상 태그를 생성하지 않고, 처리 상태(taggingStatus)를 마무리하고
 * 검색 인덱스를 갱신하는 역할만 한다.
 */
@HiltWorker
class AutoTaggingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val memoryItemDao: MemoryItemDao,
    private val memorySearchDao: MemorySearchDao,
    private val aggregateBuilder: MemoryAggregateBuilder,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val memoryId = inputData.getLong(LocalMemoryProcessingWorker.KEY_MEMORY_ID, -1L)
        if (memoryId <= 0L) return Result.failure()
        if (memoryItemDao.getById(memoryId) == null) return Result.success()

        memoryItemDao.setTaggingStatus(memoryId, StandardProcessingStatus.RUNNING, System.currentTimeMillis())
        return try {
            refreshFtsRow(memoryId, memoryItemDao, aggregateBuilder, memorySearchDao)
            memoryItemDao.setTaggingStatus(memoryId, StandardProcessingStatus.SUCCESS, System.currentTimeMillis())
            Result.success()
        } catch (t: Throwable) {
            memoryItemDao.setTaggingStatus(memoryId, StandardProcessingStatus.FAILED, System.currentTimeMillis())
            Result.failure()
        }
    }
}
