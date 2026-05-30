package com.example.snapmind.feature.memorydetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.snapmind.R
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.databinding.ActivityMemoryDetailBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailActivity : AppCompatActivity() {

    private val viewModel: DetailViewModel by viewModels()
    private lateinit var binding: ActivityMemoryDetailBinding
    private var ocrVisible = false
    private var suppressMemoTextWatcher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.bind(intent.getLongExtra(EXTRA_MEMORY_ID, -1L))

        binding.detailToolbar.setNavigationOnClickListener { finish() }
        binding.ocrHeader.setOnClickListener {
            ocrVisible = !ocrVisible
            binding.ocrText.visibility = if (ocrVisible) View.VISIBLE else View.GONE
        }
        binding.saveMemoButton.setOnClickListener {
            if (viewModel.saveMemo()) {
                Toast.makeText(this, "메모를 저장했어요.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.favoriteDetailButton.setOnClickListener { viewModel.toggleFavorite() }
        binding.deleteButton.setOnClickListener {
            viewModel.softDelete()
            Toast.makeText(this, "휴지통으로 이동했어요.", Toast.LENGTH_SHORT).show()
            finish()
        }
        binding.geminiSuggestionChip.setOnClickListener { viewModel.acceptGeminiSuggestion() }
        binding.geminiSuggestionChip.setOnCloseIconClickListener { viewModel.dismissGeminiSuggestion() }
        binding.memoEditText.doOnTextChanged { text, _, _, _ ->
            if (suppressMemoTextWatcher) return@doOnTextChanged
            viewModel.onMemoDraftChanged(text?.toString().orEmpty())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.gone) {
                        Toast.makeText(this@DetailActivity, "메모리를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
                        finish()
                        return@collect
                    }
                    val memory = state.memory ?: return@collect
                    render(memory, state.memoDraft, state.hasUnsavedMemo)
                }
            }
        }
    }

    private fun render(memory: MemoryItem, memoDraft: String, hasUnsavedMemo: Boolean) {
        binding.detailToolbar.title = memory.category.displayName
        syncMemoEditText(memoDraft)
        binding.saveMemoButton.isEnabled = hasUnsavedMemo
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

    private fun syncMemoEditText(target: String) {
        val current = binding.memoEditText.text?.toString().orEmpty()
        if (current == target) return
        suppressMemoTextWatcher = true
        binding.memoEditText.setText(target)
        binding.memoEditText.setSelection(target.length.coerceAtMost(binding.memoEditText.length()))
        suppressMemoTextWatcher = false
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

    private fun MemoryCategory.thumbnailBackground(): Int =
        when (this) {
            MemoryCategory.CODE -> R.drawable.bg_thumbnail_code
            MemoryCategory.SHOPPING -> R.drawable.bg_thumbnail_shopping
            MemoryCategory.RECEIPT -> R.drawable.bg_thumbnail_receipt
            MemoryCategory.CHAT -> R.drawable.bg_thumbnail_chat
            MemoryCategory.YOUTUBE -> R.drawable.bg_thumbnail_youtube
            MemoryCategory.TRAVEL,
            MemoryCategory.FOOD,
            MemoryCategory.DOCUMENT,
            MemoryCategory.UNKNOWN -> R.drawable.bg_thumbnail_receipt
        }

    companion object {
        private const val EXTRA_MEMORY_ID = "extra_memory_id"

        fun createIntent(context: Context, memoryId: Long): Intent =
            Intent(context, DetailActivity::class.java).putExtra(EXTRA_MEMORY_ID, memoryId)
    }
}
