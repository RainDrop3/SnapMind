package com.example.snapmind.core.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 원격 API(Vision·Gemini)로 보낼 이미지를 다운샘플링한 JPEG Base64 문자열로 인코딩한다.
 * 자동 파이프라인 워커와 상세 화면 온디맨드 추천이 같은 인코딩을 공유한다.
 */
@Singleton
class RemoteImageEncoder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun encodeBase64Jpeg(uri: Uri): String? {
        val bitmap = BitmapDecoder.decodeSampled(
            contentResolver = context.contentResolver,
            uri = uri,
            targetWidth = REMOTE_IMAGE_MAX_SIZE,
            targetHeight = REMOTE_IMAGE_MAX_SIZE,
            config = Bitmap.Config.ARGB_8888,
        ) ?: return null
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            bitmap.recycle()
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
    }

    private companion object {
        const val REMOTE_IMAGE_MAX_SIZE = 768
        const val JPEG_QUALITY = 82
    }
}
