package com.example.snapmind.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.snapmind.core.coroutine.DispatcherProvider
import com.example.snapmind.core.result.AppError
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.model.MemoryItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun export(memories: List<MemoryItem>): AppResult<Uri> =
        withContext(dispatcherProvider.io) {
            if (memories.isEmpty()) {
                return@withContext AppResult.Error(AppError.Unknown("No memories to export"))
            }
            runCatching {
                val pdf = PdfDocument()
                memories.forEachIndexed { index, memory ->
                    drawPage(pdf, memory, pageNumber = index + 1, total = memories.size)
                }

                val targetDir = File(context.cacheDir, EXPORT_SUBDIR).apply { mkdirs() }
                val fileName = "snapmind_export_${System.currentTimeMillis()}.pdf"
                val file = File(targetDir, fileName)
                FileOutputStream(file).use { pdf.writeTo(it) }
                pdf.close()

                FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file)
            }
                .map { AppResult.Success(it) as AppResult<Uri> }
                .getOrElse { error -> AppResult.Error(AppError.Unknown(error.message.orEmpty())) }
        }

    private fun drawPage(pdf: PdfDocument, memory: MemoryItem, pageNumber: Int, total: Int) {
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = pdf.startPage(info)
        val canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.GRAY
            textSize = 11f
            isAntiAlias = true
        }

        val headerText = "SnapMind · ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date())}"
        canvas.drawText(headerText, MARGIN, MARGIN + 10f, headerPaint)
        canvas.drawText("$pageNumber / $total", PAGE_WIDTH - MARGIN - 40f, MARGIN + 10f, headerPaint)

        var cursorY = MARGIN + 36f
        canvas.drawText("#${memory.id} · ${memory.category.displayName}", MARGIN, cursorY, titlePaint)
        cursorY += 22f
        if (memory.tags.isNotEmpty()) {
            canvas.drawText(memory.tags.joinToString("  "), MARGIN, cursorY, mutedPaint)
            cursorY += 18f
        }

        val bitmap = memory.imageUri?.let { decodeBitmap(it.toUri()) }
        if (bitmap != null) {
            val maxWidth = PAGE_WIDTH - 2 * MARGIN
            val maxHeight = 300f
            val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height)
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale
            val rect = RectF(MARGIN, cursorY, MARGIN + drawWidth, cursorY + drawHeight)
            canvas.drawBitmap(bitmap, null, rect, null)
            cursorY += drawHeight + 16f
            bitmap.recycle()
        }

        if (memory.memo.isNotBlank()) {
            cursorY = drawWrappedText(canvas, "📝 메모", memory.memo, bodyPaint, cursorY)
        }
        if (memory.ocrText.isNotBlank()) {
            cursorY = drawWrappedText(canvas, "🔍 OCR", memory.ocrText.take(800), bodyPaint, cursorY)
        }
        if (memory.youtubeUrl != null) {
            cursorY += 8f
            canvas.drawText("▶ ${memory.youtubeTitle ?: memory.youtubeUrl}", MARGIN, cursorY, mutedPaint)
        }

        pdf.finishPage(page)
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        label: String,
        text: String,
        paint: Paint,
        startY: Float,
    ): Float {
        val labelPaint = Paint(paint).apply { isFakeBoldText = true }
        var y = startY + 4f
        canvas.drawText(label, MARGIN, y, labelPaint)
        y += 16f
        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        text.split('\n').forEach { line ->
            wrapLine(line, paint, maxWidth).forEach { fragment ->
                if (y > PAGE_HEIGHT - MARGIN) return y
                canvas.drawText(fragment, MARGIN, y, paint)
                y += 14f
            }
        }
        return y + 6f
    }

    private fun wrapLine(line: String, paint: Paint, maxWidth: Float): List<String> {
        if (line.isEmpty()) return listOf("")
        val results = mutableListOf<String>()
        var current = StringBuilder()
        for (ch in line) {
            current.append(ch)
            if (paint.measureText(current.toString()) > maxWidth) {
                val sliced = current.substring(0, current.length - 1)
                if (sliced.isNotEmpty()) results.add(sliced)
                current = StringBuilder().append(ch)
            }
        }
        if (current.isNotEmpty()) results.add(current.toString())
        return results
    }

    private fun decodeBitmap(uri: Uri): Bitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeStream(stream, null, options)
        }
    }.getOrNull()

    companion object {
        private const val PAGE_WIDTH = 595      // A4 width in points
        private const val PAGE_HEIGHT = 842     // A4 height in points
        private const val MARGIN = 36f
        private const val EXPORT_SUBDIR = "exports"
        const val PROVIDER_AUTHORITY = "com.example.snapmind.fileprovider"
    }
}
