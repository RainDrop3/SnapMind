package com.example.snapmind.data.remote.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipdropImageUpscaleRepositoryTest {
    @Test
    fun `calculateUpscaleTarget doubles small images`() {
        val target = calculateUpscaleTarget(width = 800, height = 600)

        assertEquals(ImageDimensions(1600, 1200), target)
    }

    @Test
    fun `calculateUpscaleTarget preserves ratio within clipdrop limit`() {
        val target = calculateUpscaleTarget(width = 3000, height = 2000)

        assertEquals(ImageDimensions(4096, 2731), target)
    }

    @Test
    fun `calculateUpscaleTarget returns null when image cannot be enlarged`() {
        val target = calculateUpscaleTarget(width = 4096, height = 2500)

        assertNull(target)
    }
}
