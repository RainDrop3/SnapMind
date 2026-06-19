package com.example.snapmind.feature.memorydetail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.snapmind.MainActivity
import com.example.snapmind.R
import com.example.snapmind.core.link.YoutubeLinkHelper
import com.example.snapmind.data.model.ImageEnhancementState
import com.example.snapmind.data.model.LinkPreview
import com.example.snapmind.data.model.LinkSafetyStatus
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.databinding.ActivityMemoryDetailBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailActivity : AppCompatActivity() {

    @Inject lateinit var youtubeLinkHelper: YoutubeLinkHelper

    private val viewModel: DetailViewModel by viewModels()
    private lateinit var binding: ActivityMemoryDetailBinding
    private var ocrVisible = false
    private var suppressMemoTextWatcher = false
    private var imageEnhancementLoading = false
    private var currentMemory: MemoryItem? = null
    private var currentTags: List<String> = emptyList()
    private var currentCategories: List<MemoryCategory> = emptyList()

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
        // "저장"은 별도 변경 없이도 항시 활성화. 클릭 시 미저장 메모/태그/카테고리를 일괄 반영한다.
        binding.saveMemoButton.isEnabled = true
        binding.saveMemoButton.setOnClickListener {
            when (viewModel.save()) {
                SaveResult.SAVED -> {
                    Toast.makeText(this, "저장했어요.", Toast.LENGTH_SHORT).show()
                    goHome()
                }
                SaveResult.NO_CATEGORY ->
                    Toast.makeText(this, "카테고리를 지정해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        binding.favoriteDetailButton.setOnClickListener { viewModel.toggleFavorite() }
        binding.deleteButton.setOnClickListener {
            viewModel.softDelete()
            Toast.makeText(this, "휴지통으로 이동했어요.", Toast.LENGTH_SHORT).show()
            finish()
        }
        binding.geminiSuggestButton.setOnClickListener { viewModel.requestGeminiSuggestion() }
        binding.imageEnhancementButton.setOnClickListener { showImageEnhancementConsent() }
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
                    render(memory, state.memoDraft, state.tags, state.categories)
                }
            }
        }

        // 화면 노출 동안만 "보는 중"으로 표시 → 추천 완료 시 칩 유지, 나간 뒤 완료되면 자동 적용.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.setScreenVisible(true)
                try {
                    kotlinx.coroutines.awaitCancellation()
                } finally {
                    viewModel.setScreenVisible(false)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.geminiLoading.collect { loading ->
                    binding.geminiSuggestButton.isEnabled = !loading
                    binding.geminiSuggestButton.text =
                        if (loading) "추천 받는 중…" else "Gemini 메모 추천받기"
                    // 추천 받는 동안에는 메모 수정 불가 + 칸에 로딩 인디케이터 표시 + 저장 비활성화.
                    binding.memoEditText.isEnabled = !loading
                    binding.memoLoadingIndicator.visibility =
                        if (loading) View.VISIBLE else View.GONE
                    binding.saveMemoButton.isEnabled = !loading
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.imageEnhancementLoading.collect { loading ->
                    imageEnhancementLoading = loading
                    renderImageEnhancementButton(currentMemory)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect { message ->
                    Toast.makeText(this@DetailActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_HOME, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    private fun render(
        memory: MemoryItem,
        memoDraft: String,
        tags: List<String>,
        categories: List<MemoryCategory>,
    ) {
        currentMemory = memory
        currentTags = tags
        currentCategories = categories
        binding.detailToolbar.title = categories.firstOrNull()?.displayName ?: memory.category.displayName
        syncMemoEditText(memoDraft)
        binding.ocrText.text = memory.ocrText.ifBlank { "아직 OCR 텍스트가 준비되지 않았습니다." }
        renderPreview(memory)
        renderImageEnhancementButton(memory)
        renderCategoryChips(categories)
        renderTagChips(tags)
        binding.updatedAtText.text = "최근 수정: ${updatedAtFormat.format(Date(memory.updatedAtMillis))}"
        renderSuggestion(memory)
        renderLinkPreview(memory)
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
                .fitCenter()
                .into(detailImage)
        }
    }

    private fun renderImageEnhancementButton(memory: MemoryItem?) = with(binding.imageEnhancementButton) {
        val hasImage = !memory?.imageUri.isNullOrBlank()
        visibility = if (hasImage) View.VISIBLE else View.GONE
        isEnabled = hasImage && !imageEnhancementLoading
        text = when {
            imageEnhancementLoading -> "업그레이드 중…"
            memory?.imageEnhancementStatus == ImageEnhancementState.SUCCESS -> "화질 업그레이드 다시 실행"
            memory?.imageEnhancementStatus == ImageEnhancementState.FAILED -> "화질 업그레이드 다시 시도"
            else -> "화질 업그레이드"
        }
    }

    private fun showImageEnhancementConsent() {
        val memory = currentMemory
        if (memory?.imageUri.isNullOrBlank()) {
            Toast.makeText(this, "업그레이드할 이미지가 없어요.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("이미지를 온라인으로 업로드합니다")
            .setMessage(
                "화질 업그레이드를 위해 현재 이미지가 Clipdrop API 서버로 전송됩니다. " +
                    "민감한 정보가 포함된 이미지는 진행하지 마세요. 동의하면 업로드 후 결과 이미지를 앱 내부에 저장합니다.",
            )
            .setNegativeButton("취소", null)
            .setPositiveButton("동의하고 진행") { _, _ -> viewModel.requestImageEnhancement() }
            .show()
    }

    /** 위쪽 행: staged 카테고리. 추가/제거는 "저장" 전까지 draft 에만 반영된다. */
    private fun renderCategoryChips(categories: List<MemoryCategory>) = with(binding.detailCategoryChipGroup) {
        removeAllViews()
        categories.forEach { category ->
            addView(
                Chip(this@DetailActivity).apply {
                    text = category.displayName
                    isCheckable = false
                    isCloseIconVisible = true
                    setOnCloseIconClickListener { viewModel.onRemoveCategory(category) }
                },
            )
        }
        addView(
            Chip(this@DetailActivity).apply {
                text = "+ 카테고리"
                isCheckable = false
                setOnClickListener { showCategoryPicker() }
            },
        )
    }

    /** 아래쪽 행: staged 태그. 추가/제거는 "저장" 전까지 draft 에만 반영된다. */
    private fun renderTagChips(tags: List<String>) = with(binding.detailChipGroup) {
        removeAllViews()
        tags.forEach { tag ->
            addView(
                Chip(this@DetailActivity).apply {
                    text = tag
                    isCheckable = false
                    isCloseIconVisible = true
                    setOnCloseIconClickListener { viewModel.onRemoveTag(tag) }
                },
            )
        }
        addView(
            Chip(this@DetailActivity).apply {
                text = "+ 태그"
                isCheckable = false
                setOnClickListener { showTagPicker() }
            },
        )
    }

    /** 고정된 선택 가능 카테고리(code/Others 제외)를 단일 선택 다이얼로그로 띄워 staged 추가한다. */
    private fun showCategoryPicker() {
        val options = MemoryCategory.selectable
        val names = options.map { it.displayName }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("카테고리 추가 (최대 ${MemoryCategory.MAX_PER_MEMORY}개)")
            .setItems(names) { _, which ->
                if (!viewModel.onAddCategory(options[which])) {
                    Toast.makeText(
                        this,
                        "카테고리는 최대 ${MemoryCategory.MAX_PER_MEMORY}개까지 지정할 수 있어요.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            .show()
    }

    /** 전체 태그 목록을 다중 선택 다이얼로그로 띄워 staged 태그를 추가/해제한다("저장" 시 반영). */
    private fun showTagPicker() {
        currentMemory ?: return
        lifecycleScope.launch {
            val all = viewModel.allTagNames()
            if (all.isEmpty()) {
                Toast.makeText(
                    this@DetailActivity,
                    "등록된 태그가 없어요. 'Drawer › 태그 관리'에서 먼저 추가하세요.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            val assigned = currentTags.map { it.removePrefix("#").lowercase() }.toSet()
            val checked = BooleanArray(all.size) { index ->
                all[index].removePrefix("#").lowercase() in assigned
            }
            AlertDialog.Builder(this@DetailActivity)
                .setTitle("태그 선택")
                .setMultiChoiceItems(all.toTypedArray(), checked) { _, which, isChecked ->
                    val name = all[which]
                    if (isChecked) viewModel.onAddTag(name) else viewModel.onRemoveTag(name)
                }
                .setPositiveButton("완료", null)
                .show()
        }
    }

    private fun renderSuggestion(memory: MemoryItem) = with(binding.geminiSuggestionChip) {
        val suggestion = memory.geminiSuggestion
        visibility = if (suggestion.isNullOrBlank()) View.GONE else View.VISIBLE
        text = if (suggestion.isNullOrBlank()) "" else "Gemini 제안: $suggestion"
    }

    private fun renderLinkPreview(memory: MemoryItem) = with(binding) {
        val preview = memory.linkPreview
        linkPreviewCard.visibility = if (preview == null) View.GONE else View.VISIBLE
        if (preview == null) {
            linkPreviewCard.setOnClickListener(null)
            return@with
        }

        val site = preview.siteName ?: preview.url.toHostLabel()
        linkPreviewSite.visibility = if (site.isNullOrBlank()) View.GONE else View.VISIBLE
        linkPreviewSite.text = site.orEmpty()
        linkPreviewTitle.text = preview.title ?: site ?: preview.url
        linkPreviewDescription.visibility =
            if (preview.description.isNullOrBlank()) View.GONE else View.VISIBLE
        linkPreviewDescription.text = preview.description.orEmpty()
        linkPreviewUrl.text = preview.url
        renderLinkSafety(preview)

        if (preview.imageUrl.isNullOrBlank()) {
            linkPreviewImage.visibility = View.GONE
            linkPreviewImage.setImageDrawable(null)
        } else {
            linkPreviewImage.visibility = View.VISIBLE
            Glide.with(linkPreviewImage)
                .load(preview.imageUrl)
                .centerCrop()
                .into(linkPreviewImage)
        }
        linkPreviewCard.setOnClickListener {
            confirmAndOpenLink(preview)
        }
    }

    private fun renderLinkSafety(preview: LinkPreview) = with(binding) {
        val warning = when (preview.safetyStatus) {
            LinkSafetyStatus.UNSAFE -> "위험 가능성이 있는 링크"
            LinkSafetyStatus.CHECK_FAILED -> "안전 확인 실패"
            else -> null
        }
        linkSafetyWarning.visibility = if (warning == null) View.GONE else View.VISIBLE
        linkSafetyWarning.text = when {
            warning == null -> ""
            preview.safetyThreatTypes.isNullOrBlank() -> warning
            else -> "$warning · ${preview.safetyThreatTypes}"
        }
        linkPreviewCard.strokeColor = ContextCompat.getColor(
            this@DetailActivity,
            if (preview.safetyStatus == LinkSafetyStatus.UNSAFE) R.color.snap_rose else R.color.snap_outline,
        )
    }

    private fun confirmAndOpenLink(preview: LinkPreview) {
        if (preview.safetyStatus != LinkSafetyStatus.UNSAFE) {
            openLink(preview.url)
            return
        }
        AlertDialog.Builder(this)
            .setTitle("위험 가능성이 있는 링크")
            .setMessage("Safe Browsing에서 ${preview.safetyThreatTypes ?: "위험"} 항목으로 감지했습니다. 그래도 열까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("열기") { _, _ -> openLink(preview.url) }
            .show()
    }

    private fun openLink(url: String) {
        val openUrl = youtubeLinkHelper.watchUrl(url) ?: url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(openUrl))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "링크를 열 수 있는 앱이 없어요.", Toast.LENGTH_SHORT).show()
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "링크 형식이 올바르지 않아요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun String.toHostLabel(): String? =
        runCatching { Uri.parse(this).host?.removePrefix("www.") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    private fun MemoryCategory.thumbnailBackground(): Int =
        when (this) {
            MemoryCategory.RECEIPT -> R.drawable.bg_thumbnail_receipt
            MemoryCategory.CHAT -> R.drawable.bg_thumbnail_chat
            MemoryCategory.YOUTUBE -> R.drawable.bg_thumbnail_youtube
            MemoryCategory.TRAVEL,
            MemoryCategory.FOOD,
            MemoryCategory.DOCUMENT,
            MemoryCategory.OTHERS -> R.drawable.bg_thumbnail_receipt
        }

    companion object {
        private const val EXTRA_MEMORY_ID = "extra_memory_id"
        private val updatedAtFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)

        fun createIntent(context: Context, memoryId: Long): Intent =
            Intent(context, DetailActivity::class.java).putExtra(EXTRA_MEMORY_ID, memoryId)
    }
}
