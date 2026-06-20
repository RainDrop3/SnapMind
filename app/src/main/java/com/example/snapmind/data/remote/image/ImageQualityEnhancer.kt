package com.example.snapmind.data.remote.image

import androidx.core.net.toUri
import com.example.snapmind.core.result.AppError
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.core.settings.AppPreferences
import com.example.snapmind.data.local.dao.MemoryItemDao
import com.example.snapmind.data.local.entity.ImageEnhancementStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageQualityEnhancer @Inject constructor(
    private val memoryItemDao: MemoryItemDao,
    private val appPreferences: AppPreferences,
    private val upscaleRepository: ClipdropImageUpscaleRepository,
) {
    suspend fun enhance(memoryId: Long): AppResult<Unit> {
        val flags = appPreferences.current()
        if (!flags.imageEnhancementEnabled || flags.clipdropApiKey.isBlank()) {
            return AppResult.Error(AppError.RemoteFeatureDisabled)
        }

        val entity = memoryItemDao.getById(memoryId)
            ?: return AppResult.Error(AppError.FileNotFound)
        val imageUri = entity.imageUri.takeIf { it.isNotBlank() }?.toUri()
            ?: return AppResult.Error(AppError.FileNotFound)

        memoryItemDao.setImageEnhancementStatus(
            memoryId = memoryId,
            status = ImageEnhancementStatus.RUNNING,
            updatedAt = System.currentTimeMillis(),
        )

        return when (val result = upscaleRepository.upscale(imageUri, entity.mimeType, flags.clipdropApiKey)) {
            is AppResult.Success -> {
                val now = System.currentTimeMillis()
                val affected = memoryItemDao.applyEnhancedImage(
                    memoryId = memoryId,
                    imageUri = result.data.imageUri,
                    mimeType = result.data.mimeType,
                    originalImageUri = entity.originalImageUri ?: entity.imageUri,
                    status = ImageEnhancementStatus.SUCCESS,
                    provider = result.data.provider,
                    enhancedAt = now,
                    updatedAt = now,
                )
                if (affected > 0) AppResult.Success(Unit)
                else AppResult.Error(AppError.Unknown("업그레이드 결과를 메모리에 반영하지 못했어요."))
            }
            is AppResult.Error -> {
                memoryItemDao.setImageEnhancementStatus(
                    memoryId = memoryId,
                    status = ImageEnhancementStatus.FAILED,
                    updatedAt = System.currentTimeMillis(),
                )
                AppResult.Error(result.error)
            }
        }
    }
}
