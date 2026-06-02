package com.example.snapmind.feature.importimage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.snapmind.MainActivity
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivityShareImportBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivityShareImportBinding
    private val drafts = mutableListOf<ImportDraft>()
    private var currentIndex = 0
    private var suppressDraftWatcher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drafts += intent.imageUris().map { uri ->
            ImportDraft(
                uri = uri,
                mimeType = contentResolver.getType(uri) ?: intent.getStringExtra(EXTRA_INTERNAL_MIME_TYPE) ?: intent.type,
            )
        }
        if (drafts.isEmpty()) {
            Toast.makeText(this, "이미지 공유만 지원합니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        renderPreview()
        binding.shareToolbar.setNavigationOnClickListener { finish() }
        binding.cancelButton.setOnClickListener { finish() }
        binding.saveButton.setOnClickListener { saveSharedImages() }
        binding.previousImageButton.setOnClickListener { moveDraft(-1) }
        binding.nextImageButton.setOnClickListener { moveDraft(1) }
        binding.importMemoEditText.doOnTextChanged { text, _, _, _ ->
            if (!suppressDraftWatcher) drafts[currentIndex].memo = text?.toString().orEmpty()
        }
        binding.importTagsEditText.doOnTextChanged { text, _, _, _ ->
            if (!suppressDraftWatcher) drafts[currentIndex].tags = text?.toString().orEmpty()
        }
    }

    private fun renderPreview() = with(binding) {
        val draft = drafts[currentIndex]
        Glide.with(sharePreview)
            .load(draft.uri)
            .thumbnail(0.25f)
            .centerCrop()
            .into(sharePreview)
        shareTitle.text = if (drafts.size == 1) "저장 전 편집" else "저장 전 편집 ${currentIndex + 1}/${drafts.size}"
        shareMeta.text = "${drafts.size}개 이미지 · ${draft.mimeType ?: "image/*"}"
        imageStepControls.visibility = if (drafts.size > 1) android.view.View.VISIBLE else android.view.View.GONE
        previousImageButton.isEnabled = currentIndex > 0
        nextImageButton.isEnabled = currentIndex < drafts.lastIndex
        saveButton.text = if (drafts.size == 1) "저장하고 분석" else "모두 저장"
        suppressDraftWatcher = true
        importMemoEditText.setText(draft.memo)
        importMemoEditText.setSelection(draft.memo.length.coerceAtMost(importMemoEditText.length()))
        importTagsEditText.setText(draft.tags)
        importTagsEditText.setSelection(draft.tags.length.coerceAtMost(importTagsEditText.length()))
        suppressDraftWatcher = false
    }

    private fun moveDraft(delta: Int) {
        val next = (currentIndex + delta).coerceIn(0, drafts.lastIndex)
        if (next == currentIndex) return
        currentIndex = next
        renderPreview()
    }

    private fun saveSharedImages() {
        binding.saveButton.isEnabled = false
        lifecycleScope.launch {
            var successCount = 0
            drafts.forEach { draft ->
                val result = memoryRepository.importImage(
                    sourceUri = draft.uri,
                    mimeType = draft.mimeType,
                    sourceLabel = intent.getStringExtra(EXTRA_SOURCE_LABEL) ?: callingPackage ?: "공유 이미지",
                    initialMemo = draft.memo,
                    initialTags = draft.tags.parseTags(),
                )
                if (result is AppResult.Success) {
                    successCount += 1
                }
            }

            if (successCount == 0) {
                binding.saveButton.isEnabled = true
                Toast.makeText(this@ShareActivity, "저장할 수 있는 이미지가 없어요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ShareActivity, "${successCount}개 이미지를 저장했어요.", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this@ShareActivity, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_OPEN_HOME, true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
                finish()
            }
        }
    }

    private fun Intent.imageUris(): List<Uri> =
        getParcelableExtra<Uri>(EXTRA_INTERNAL_URI)?.let { listOf(it) } ?: when (action) {
            Intent.ACTION_SEND -> listOfNotNull(getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE -> getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            else -> emptyList()
        }

    private fun String.parseTags(): List<String> =
        split(Regex("[,\\s]+"))
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

    private data class ImportDraft(
        val uri: Uri,
        val mimeType: String?,
        var memo: String = "",
        var tags: String = "",
    )

    companion object {
        private const val EXTRA_INTERNAL_URI = "extra_internal_uri"
        private const val EXTRA_INTERNAL_MIME_TYPE = "extra_internal_mime_type"
        private const val EXTRA_SOURCE_LABEL = "extra_source_label"

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
