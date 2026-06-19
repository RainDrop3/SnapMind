package com.example.snapmind.data.remote.image

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import com.example.snapmind.core.image.ImageImporter
import com.example.snapmind.core.result.AppError
import com.example.snapmind.core.result.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

data class EnhancedImageResult(
    val imageUri: String,
    val mimeType: String,
    val byteSize: Long,
    val provider: String = ClipdropImageUpscaleRepository.PROVIDER,
)

data class ImageDimensions(val width: Int, val height: Int)

@Singleton
class ClipdropImageUpscaleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ClipdropImageUpscaleApiService,
) {
    suspend fun upscale(
        imageUri: Uri,
        mimeType: String?,
        apiKey: String,
    ): AppResult<EnhancedImageResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext AppResult.Error(AppError.RemoteFeatureDisabled)

        val dimensions = readDimensions(imageUri)
            ?: return@withContext AppResult.Error(AppError.FileNotFound)
        val target = calculateUpscaleTarget(dimensions.width, dimensions.height)
            ?: return@withContext AppResult.Error(AppError.Unknown("이미지가 이미 충분히 커서 업그레이드 목표 크기를 만들 수 없어요."))
        val upload = prepareUploadFile(imageUri, mimeType)
            ?: return@withContext AppResult.Error(AppError.UnsupportedImageType)

        try {
            val response = api.upscale(
                apiKey = apiKey,
                imageFile = MultipartBody.Part.createFormData(
                    name = "image_file",
                    filename = upload.file.name,
                    body = upload.file.asRequestBody(upload.mimeType.toMediaType()),
                ),
                targetWidth = target.width.toString().toRequestBody(TEXT_PLAIN),
                targetHeight = target.height.toString().toRequestBody(TEXT_PLAIN),
            )
            if (!response.isSuccessful) {
                return@withContext AppResult.Error(response.toAppError())
            }
            val body = response.body()
                ?: return@withContext AppResult.Error(AppError.Unknown("업그레이드 결과가 비어 있어요."))
            val resultMime = body.contentType()?.let { "${it.type}/${it.subtype}".lowercase(Locale.US) }
                ?.takeIf { it in RESULT_MIME_TYPES }
                ?: DEFAULT_RESULT_MIME
            val targetFile = createResultFile(resultMime)
            val byteSize = body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
                targetFile.length()
            }
            if (byteSize <= 0L) {
                targetFile.delete()
                return@withContext AppResult.Error(AppError.Unknown("업그레이드 결과를 저장하지 못했어요."))
            }
            AppResult.Success(
                EnhancedImageResult(
                    imageUri = targetFile.toUri().toString(),
                    mimeType = resultMime,
                    byteSize = byteSize,
                ),
            )
        } catch (_: SocketTimeoutException) {
            AppResult.Error(AppError.ApiTimeout)
        } catch (_: java.io.IOException) {
            AppResult.Error(AppError.NetworkUnavailable)
        } catch (error: Exception) {
            AppResult.Error(AppError.Unknown(error.message.orEmpty()))
        } finally {
            if (upload.isTemporary) upload.file.delete()
        }
    }

    private fun readDimensions(uri: Uri): ImageDimensions? = runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        if (options.outWidth > 0 && options.outHeight > 0) {
            ImageDimensions(options.outWidth, options.outHeight)
        } else {
            null
        }
    }.getOrNull()

    private fun prepareUploadFile(uri: Uri, mimeType: String?): UploadFile? {
        val resolvedMime = resolveUploadMime(uri, mimeType) ?: return null
        val file = uri.toLocalFile()
        if (file != null && file.exists() && file.length() > 0L) {
            return UploadFile(file = file, mimeType = resolvedMime, isTemporary = false)
        }

        val tempFile = File(context.cacheDir, "snapmind_upscale_${UUID.randomUUID()}.${
            resolvedMime.fileExtension()
        }")
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: return null
            if (tempFile.length() <= 0L) {
                tempFile.delete()
                null
            } else {
                UploadFile(file = tempFile, mimeType = resolvedMime, isTemporary = true)
            }
        }.getOrElse {
            tempFile.delete()
            null
        }
    }

    private fun resolveUploadMime(uri: Uri, explicit: String?): String? {
        val candidates = listOfNotNull(
            explicit,
            context.contentResolver.getType(uri),
            uri.lastPathSegment?.substringAfterLast('.', missingDelimiterValue = "")?.let { ext ->
                EXTENSION_MIME[ext.lowercase(Locale.US)]
            },
        )
        return candidates
            .map { it.lowercase(Locale.US).normalizeMime() }
            .firstOrNull { it in UPLOAD_MIME_TYPES }
    }

    private fun Uri.toLocalFile(): File? {
        if (scheme != "file") return null
        val path = path ?: return null
        return File(path)
    }

    private fun createResultFile(mimeType: String): File {
        val dir = File(context.filesDir, ImageImporter.IMAGE_SUBDIR).apply { mkdirs() }
        val suffix = UUID.randomUUID().toString().substring(0, 8)
        return File(dir, "memory_upscaled_${System.currentTimeMillis()}_$suffix.${mimeType.fileExtension()}")
    }

    private fun retrofit2.Response<*>.toAppError(): AppError {
        val body = runCatching { errorBody()?.string() }.getOrNull()
        return when (code()) {
            401, 403 -> AppError.ApiUnauthorized
            402, 429 -> AppError.ApiQuotaExceeded
            408, 504 -> AppError.ApiTimeout
            else -> AppError.Http(code(), body)
        }
    }

    private data class UploadFile(
        val file: File,
        val mimeType: String,
        val isTemporary: Boolean,
    )

    companion object {
        const val PROVIDER = "clipdrop"
        private const val DEFAULT_RESULT_MIME = "image/jpeg"
        private val TEXT_PLAIN = "text/plain".toMediaType()
        private val UPLOAD_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private val RESULT_MIME_TYPES = setOf("image/jpeg", "image/webp")
        private val EXTENSION_MIME = mapOf(
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "webp" to "image/webp",
        )
    }
}

internal fun calculateUpscaleTarget(width: Int, height: Int): ImageDimensions? {
    if (width <= 0 || height <= 0) return null
    val scale = minOf(
        2f,
        ClipdropTarget.MAX_SIZE.toFloat() / width,
        ClipdropTarget.MAX_SIZE.toFloat() / height,
    )
    if (scale <= 1.01f) return null
    return ImageDimensions(
        width = (width * scale).roundToInt().coerceIn(1, ClipdropTarget.MAX_SIZE),
        height = (height * scale).roundToInt().coerceIn(1, ClipdropTarget.MAX_SIZE),
    )
}

private object ClipdropTarget {
    const val MAX_SIZE = 4096
}

private fun String.normalizeMime(): String =
    if (this == "image/jpg") "image/jpeg" else this

private fun String.fileExtension(): String =
    when (normalizeMime()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
