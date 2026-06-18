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
    val visionEnabled: Boolean,
    val geminiEnabled: Boolean,
    val youtubeEnabled: Boolean,
    val visionApiKey: String,
    val geminiApiKey: String,
    val youtubeApiKey: String,
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var visionEnabled: Boolean
        get() = prefs.getBoolean(KEY_VISION, true)
        set(value) { prefs.edit().putBoolean(KEY_VISION, value).apply() }

    var geminiEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEMINI, true)
        set(value) { prefs.edit().putBoolean(KEY_GEMINI, value).apply() }

    var youtubeEnabled: Boolean
        get() = prefs.getBoolean(KEY_YOUTUBE, true)
        set(value) { prefs.edit().putBoolean(KEY_YOUTUBE, value).apply() }

    // API 키는 사용자 입력이 아니라 빌드 시 BuildConfig 에 내장된 개발자 키를 사용한다.
    // (값 공급: local.properties → app/build.gradle.kts → BuildConfig)
    val visionApiKey: String get() = BuildConfig.VISION_API_KEY.trim()

    val geminiApiKey: String get() = BuildConfig.GEMINI_API_KEY.trim()

    val youtubeApiKey: String get() = BuildConfig.YOUTUBE_API_KEY.trim()

    fun current(): RemoteFeatureFlags = RemoteFeatureFlags(
        visionEnabled = visionEnabled,
        geminiEnabled = geminiEnabled,
        youtubeEnabled = youtubeEnabled,
        visionApiKey = visionApiKey,
        geminiApiKey = geminiApiKey,
        youtubeApiKey = youtubeApiKey,
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
        private const val KEY_VISION = "vision_enabled"
        private const val KEY_GEMINI = "gemini_enabled"
        private const val KEY_YOUTUBE = "youtube_enabled"
    }
}
