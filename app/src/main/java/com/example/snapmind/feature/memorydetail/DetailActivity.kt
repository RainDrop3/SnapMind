package com.example.snapmind.feature.memorydetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.snapmind.R
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivityMemoryDetailBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivityMemoryDetailBinding
    private var memoryId: Long = -1L
    private var ocrVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        memoryId = intent.getLongExtra(EXTRA_MEMORY_ID, -1L)

        binding.detailToolbar.setNavigationOnClickListener { finish() }
        binding.ocrHeader.setOnClickListener {
            ocrVisible = !ocrVisible
            binding.ocrText.visibility = if (ocrVisible) View.VISIBLE else View.GONE
        }
        binding.saveMemoButton.setOnClickListener { saveMemo() }
        binding.favoriteDetailButton.setOnClickListener {
            memoryRepository.toggleFavorite(memoryId)
            render()
        }
        binding.deleteButton.setOnClickListener {
            memoryRepository.softDelete(memoryId)
            Toast.makeText(this, "휴지통으로 이동했어요.", Toast.LENGTH_SHORT).show()
            finish()
        }
        binding.geminiSuggestionChip.setOnClickListener {
            memoryRepository.acceptGeminiSuggestion(memoryId)
            render()
        }
        binding.geminiSuggestionChip.setOnCloseIconClickListener {
            memoryRepository.dismissGeminiSuggestion(memoryId)
            render()
        }

        lifecycleScope.launch {
            memoryRepository.memories.collect { render() }
        }
    }

    private fun render() {
        val memory = memoryRepository.getMemory(memoryId)
        if (memory == null || memory.isDeleted) {
            Toast.makeText(this, "메모리를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.detailToolbar.title = memory.category.displayName
        binding.memoEditText.setText(memory.memo)
        binding.ocrText.text = memory.ocrText.ifBlank { "아직 OCR 텍스트가 준비되지 않았습니다." }
        renderPreview(memory)
        renderChips(memory)
        renderSuggestion(memory)
        renderYoutube(memory)
        binding.favoriteDetailButton.iconTint = ContextCompat.getColorStateList(
            this,
            if (memory.isFavorite) R.color.snap_rose else R.color.snap_text_secondary,
        )
    }

    private fun renderPreview(memory: MemoryItem) = with(binding) {
        detailPreviewFrame.setBackgroundResource(memory.category.thumbnailBackground())
        if (memory.imageUri.isNullOrBlank()) {
            detailImage.setImageDrawable(null)
            detailGlyph.text = memory.category.glyph
        } else {
            detailGlyph.text = ""
            Glide.with(detailImage)
                .load(Uri.parse(memory.imageUri))
                .thumbnail(0.25f)
                .centerCrop()
                .into(detailImage)
        }
    }

    private fun renderChips(memory: MemoryItem) = with(binding.detailChipGroup) {
        removeAllViews()
        addView(
            Chip(this@DetailActivity).apply {
                text = memory.category.displayName
                isCheckable = false
            },
        )
        memory.tags.forEach { tag ->
            addView(
                Chip(this@DetailActivity).apply {
                    text = tag
                    isCheckable = false
                },
            )
        }
    }

    private fun renderSuggestion(memory: MemoryItem) = with(binding.geminiSuggestionChip) {
        val suggestion = memory.geminiSuggestion
        visibility = if (suggestion.isNullOrBlank()) View.GONE else View.VISIBLE
        text = if (suggestion.isNullOrBlank()) "" else "Gemini 제안: $suggestion"
    }

    private fun renderYoutube(memory: MemoryItem) = with(binding.youtubeButton) {
        visibility = if (memory.youtubeUrl.isNullOrBlank()) View.GONE else View.VISIBLE
        text = memory.youtubeTitle?.let { "영상 바로 이동: $it" } ?: "영상 바로 이동"
        setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(memory.youtubeUrl)))
        }
    }

    private fun saveMemo() {
        memoryRepository.updateMemo(memoryId, binding.memoEditText.text?.toString().orEmpty())
        Toast.makeText(this, "메모를 저장했어요.", Toast.LENGTH_SHORT).show()
    }

    private fun MemoryCategory.thumbnailBackground(): Int =
        when (this) {
            MemoryCategory.CODE -> R.drawable.bg_thumbnail_code
            MemoryCategory.SHOPPING -> R.drawable.bg_thumbnail_shopping
            MemoryCategory.MAP -> R.drawable.bg_thumbnail_map
            MemoryCategory.RECEIPT -> R.drawable.bg_thumbnail_receipt
            MemoryCategory.CHAT -> R.drawable.bg_thumbnail_chat
            MemoryCategory.YOUTUBE -> R.drawable.bg_thumbnail_youtube
            MemoryCategory.DOCUMENT,
            MemoryCategory.UNKNOWN -> R.drawable.bg_thumbnail_receipt
        }

    companion object {
        private const val EXTRA_MEMORY_ID = "extra_memory_id"

        fun createIntent(context: Context, memoryId: Long): Intent =
            Intent(context, DetailActivity::class.java).putExtra(EXTRA_MEMORY_ID, memoryId)
    }
}
