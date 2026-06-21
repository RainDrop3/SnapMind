package com.example.snapmind.data.repository

import com.example.snapmind.data.local.entity.GeminiMemoStatus
import com.example.snapmind.data.local.entity.MemoryItemEntity
import com.example.snapmind.data.local.entity.OptionalRemoteProcessingStatus
import com.example.snapmind.data.local.entity.StandardProcessingStatus
import com.example.snapmind.data.model.ProcessingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {

    @Test
    fun `core processing failure is exposed as error`() {
        val entity = completedEntity().copy(ocrStatus = StandardProcessingStatus.FAILED)

        assertEquals(ProcessingStatus.ERROR, entity.composeProcessingStatus())
    }

    @Test
    fun `optional remote failures do not expose a card error`() {
        val entity = completedEntity().copy(
            visionLabelStatus = OptionalRemoteProcessingStatus.FAILED,
            youtubeLinkStatus = OptionalRemoteProcessingStatus.FAILED,
            geminiMemoStatus = GeminiMemoStatus.FAILED,
        )

        assertEquals(ProcessingStatus.DONE, entity.composeProcessingStatus())
    }

    @Test
    fun `pending core processing remains processing`() {
        val entity = completedEntity().copy(taggingStatus = StandardProcessingStatus.PENDING)

        assertEquals(ProcessingStatus.PROCESSING, entity.composeProcessingStatus())
    }

    private fun completedEntity() = MemoryItemEntity(
        imageUri = "file:///memory.jpg",
        createdAt = 1L,
        updatedAt = 1L,
        ocrStatus = StandardProcessingStatus.SUCCESS,
        classificationStatus = StandardProcessingStatus.SUCCESS,
        taggingStatus = StandardProcessingStatus.SUCCESS,
        visionLabelStatus = OptionalRemoteProcessingStatus.SUCCESS,
        youtubeLinkStatus = OptionalRemoteProcessingStatus.SUCCESS,
        geminiMemoStatus = GeminiMemoStatus.ACCEPTED,
    )
}
