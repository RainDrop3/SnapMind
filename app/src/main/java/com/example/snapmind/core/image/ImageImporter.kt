package com.example.snapmind.core.image

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.example.snapmind.core.coroutine.DispatcherProvider
import com.example.snapmind.core.result.AppError
import com.example.snapmind.core.result.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

data class ImportedImage(
    val targetUri: String,
    val sourceUri: String,
    val mimeType: String,
    val contentHash: String?,
    val byteSize: Long,
)

@Singleton
class ImageImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun import(
        sourceUri: Uri,
        explicitMimeType: String?,
    ): AppResult<ImportedImage> = withContext(dispatcherProvider.io) {
        val resolvedMime = resolveMimeType(sourceUri, explicitMimeType)
        if (resolvedMime == null || !ACCEPTED_MIME_TYPES.contains(resolvedMime)) {
            return@withContext AppResult.Error(AppError.UnsupportedImageType)
        }

        val targetDir = File(context.filesDir, IMAGE_SUBDIR).apply { mkdirs() }
        val fileName = buildFileName(resolvedMime)
        val targetFile = File(targetDir, fileName)

        val digest = MessageDigest.getInstance("SHA-256")
        val byteCount = runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                        total += read
                    }
                    total
                }
            } ?: return@withContext AppResult.Error(AppError.FileNotFound)
        }.getOrElse { error ->
            targetFile.delete()
            return@withContext when (error) {
                is SecurityException -> AppResult.Error(AppError.PermissionDenied)
                is java.io.FileNotFoundException -> AppResult.Error(AppError.FileNotFound)
                else -> AppResult.Error(AppError.Unknown(error.message.orEmpty()))
            }
        }

        if (byteCount <= 0L) {
            targetFile.delete()
            return@withContext AppResult.Error(AppError.FileNotFound)
        }

        val hashHex = digest.digest().joinToString(separator = "") { "%02x".format(it) }

        AppResult.Success(
            ImportedImage(
                targetUri = targetFile.toUri().toString(),
                sourceUri = sourceUri.toString(),
                mimeType = resolvedMime,
                contentHash = hashHex,
                byteSize = byteCount,
            )
        )
    }

    /**
     * 가져오기 과정에서 생성했지만 DB에 연결되지 않은 앱 전용 복사본을 정리한다.
     * filesDir/snapmind/images 밖의 파일은 절대 삭제하지 않는다.
     */
    suspend fun deleteOwnedCopy(targetUri: String): Boolean = withContext(dispatcherProvider.io) {
        val uri = runCatching { targetUri.toUri() }.getOrNull() ?: return@withContext false
        if (uri.scheme != "file") return@withContext false
        val path = uri.path ?: return@withContext false
        val ownedDir = File(context.filesDir, IMAGE_SUBDIR).canonicalFile
        val target = runCatching { File(path).canonicalFile }.getOrNull() ?: return@withContext false
        val isOwned = target.parentFile?.canonicalFile == ownedDir
        if (!isOwned) return@withContext false
        !target.exists() || target.delete()
    }

    private fun resolveMimeType(sourceUri: Uri, explicit: String?): String? {
        val candidate = explicit ?: context.contentResolver.getType(sourceUri) ?: return null
        return candidate.lowercase()
    }

    private fun buildFileName(mime: String): String {
        val ext = MIME_EXTENSION[mime] ?: "jpg"
        val suffix = UUID.randomUUID().toString().substring(0, 8)
        return "memory_${System.currentTimeMillis()}_$suffix.$ext"
    }

    companion object {
        const val IMAGE_SUBDIR = "snapmind/images"
        private const val BUFFER_SIZE = 8 * 1024

        private val MIME_EXTENSION = mapOf(
            "image/jpeg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
            "image/heic" to "heic",
            "image/heif" to "heif",
        )

        val ACCEPTED_MIME_TYPES: Set<String> = MIME_EXTENSION.keys
    }
}
