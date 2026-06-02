package com.example.snapmind.feature.importimage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.snapmind.MainActivity
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.model.ProcessingStatus
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivityShareImportBinding
import com.example.snapmind.feature.memorydetail.DetailActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class ShareActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivityShareImportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uris = intent.imageUris()
        if (uris.isEmpty()) {
            Toast.makeText(this, "이미지 공유만 지원합니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Glide.with(binding.sharePreview).load(uris.first()).centerCrop().into(binding.sharePreview)
        binding.shareToolbar.setNavigationOnClickListener { finish() }

        importAndOpen(uris)
    }

    private fun importAndOpen(uris: List<Uri>) {
        val sourceLabel = intent.getStringExtra(EXTRA_SOURCE_LABEL) ?: callingPackage ?: "공유 이미지"
        lifecycleScope.launch {
            val importedIds = mutableListOf<Long>()
            uris.forEach { uri ->
                val result = memoryRepository.importImage(
                    sourceUri = uri,
                    mimeType = contentResolver.getType(uri)
                        ?: intent.getStringExtra(EXTRA_INTERNAL_MIME_TYPE)
                        ?: intent.type,
                    sourceLabel = sourceLabel,
                )
                if (result is AppResult.Success) importedIds += result.data.id
            }

            if (importedIds.isEmpty()) {
                Toast.makeText(this@ShareActivity, "저장할 수 있는 이미지가 없어요.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            // 분석(OCR · 분류 · 태깅)이 끝날 때까지 기다린 뒤 화면을 전환한다.
            awaitProcessed(importedIds.toSet())

            if (importedIds.size == 1) {
                startActivity(DetailActivity.createIntent(this@ShareActivity, importedIds.first()))
            } else {
                Toast.makeText(this@ShareActivity, "${importedIds.size}개 이미지를 저장했어요.", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this@ShareActivity, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_OPEN_HOME, true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
            }
            finish()
        }
    }

    /** 대상 메모리들이 모두 처리중(PROCESSING) 상태를 벗어날 때까지 대기. 지연되면 타임아웃 후 진행. */
    private suspend fun awaitProcessed(ids: Set<Long>) {
        withTimeoutOrNull(ANALYSIS_TIMEOUT_MS) {
            memoryRepository.memories.first { memories ->
                ids.all { id ->
                    val memory = memories.firstOrNull { it.id == id }
                    memory != null && memory.processingStatus != ProcessingStatus.PROCESSING
                }
            }
        }
    }

    private fun Intent.imageUris(): List<Uri> =
        getParcelableExtra<Uri>(EXTRA_INTERNAL_URI)?.let { listOf(it) } ?: when (action) {
            Intent.ACTION_SEND -> listOfNotNull(getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE -> getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            else -> emptyList()
        }

    companion object {
        private const val EXTRA_INTERNAL_URI = "extra_internal_uri"
        private const val EXTRA_INTERNAL_MIME_TYPE = "extra_internal_mime_type"
        private const val EXTRA_SOURCE_LABEL = "extra_source_label"
        private const val ANALYSIS_TIMEOUT_MS = 25_000L

        fun createIntent(
            context: Context,
            uri: Uri,
            mimeType: String?,
            sourceLabel: String,
        ): Intent = Intent(context, ShareActivity::class.java)
            .putExtra(EXTRA_INTERNAL_URI, uri)
            .putExtra(EXTRA_INTERNAL_MIME_TYPE, mimeType)
            .putExtra(EXTRA_SOURCE_LABEL, sourceLabel)
    }
}
