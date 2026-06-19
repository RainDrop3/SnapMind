package com.example.snapmind.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.snapmind.BuildConfig
import com.example.snapmind.core.pdf.PdfExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class RemoteFeatureFlags(
    val linkPreviewEnabled: Boolean,
    val youtubeEnabled: Boolean,
    val safeBrowsingEnabled: Boolean,
    val imageEnhancementEnabled: Boolean,
    val geminiEnabled: Boolean,
    val geminiApiKey: String,
    val youtubeApiKey: String,
    val safeBrowsingApiKey: String,
    val clipdropApiKey: String,
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var linkPreviewEnabled: Boolean
        get() = prefs.getBoolean(KEY_LINK_PREVIEW, prefs.getBoolean(KEY_VISION_LEGACY, true))
        set(value) { prefs.edit().putBoolean(KEY_LINK_PREVIEW, value).apply() }

    var youtubeEnabled: Boolean
        get() = prefs.getBoolean(KEY_YOUTUBE, true)
        set(value) { prefs.edit().putBoolean(KEY_YOUTUBE, value).apply() }

    var safeBrowsingEnabled: Boolean
        get() = prefs.getBoolean(KEY_SAFE_BROWSING, true)
        set(value) { prefs.edit().putBoolean(KEY_SAFE_BROWSING, value).apply() }

    var imageEnhancementEnabled: Boolean
        get() = prefs.getBoolean(KEY_IMAGE_ENHANCEMENT, true)
        set(value) { prefs.edit().putBoolean(KEY_IMAGE_ENHANCEMENT, value).apply() }

    var geminiEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEMINI, true)
        set(value) { prefs.edit().putBoolean(KEY_GEMINI, value).apply() }

    // API 키는 사용자 입력이 아니라 빌드 시 BuildConfig 에 내장된 개발자 키를 사용한다.
    // (값 공급: local.properties → app/build.gradle.kts → BuildConfig)
    val geminiApiKey: String get() = BuildConfig.GEMINI_API_KEY.trim()
    val youtubeApiKey: String get() = BuildConfig.YOUTUBE_API_KEY.trim()
    val safeBrowsingApiKey: String get() = BuildConfig.SAFE_BROWSING_API_KEY.trim()
    val clipdropApiKey: String get() = BuildConfig.CLIPDROP_API_KEY.trim()

    fun current(): RemoteFeatureFlags = RemoteFeatureFlags(
        linkPreviewEnabled = linkPreviewEnabled,
        youtubeEnabled = youtubeEnabled,
        safeBrowsingEnabled = safeBrowsingEnabled,
        imageEnhancementEnabled = imageEnhancementEnabled,
        geminiEnabled = geminiEnabled,
        geminiApiKey = geminiApiKey,
        youtubeApiKey = youtubeApiKey,
        safeBrowsingApiKey = safeBrowsingApiKey,
        clipdropApiKey = clipdropApiKey,
    )

    fun observe(): Flow<RemoteFeatureFlags> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(current())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(current())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun clearPdfCache(): Long {
        val dir = File(context.cacheDir, PdfExporter.EXPORT_SUBDIR_NAME)
        if (!dir.exists()) return 0L
        var freed = 0L
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".pdf")) {
                freed += file.length()
                file.delete()
            }
        }
        return freed
    }

    companion object {
        private const val PREFS_NAME = "snapmind_prefs"
        private const val KEY_LINK_PREVIEW = "link_preview_enabled"
        private const val KEY_VISION_LEGACY = "vision_enabled"
        private const val KEY_YOUTUBE = "youtube_enabled"
        private const val KEY_SAFE_BROWSING = "safe_browsing_enabled"
        private const val KEY_IMAGE_ENHANCEMENT = "image_enhancement_enabled"
        private const val KEY_GEMINI = "gemini_enabled"
    }
}
