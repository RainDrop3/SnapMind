package com.example.snapmind.feature.home

import android.net.Uri
import android.text.format.DateUtils
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.snapmind.R
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.model.ProcessingStatus
import com.example.snapmind.databinding.ItemMemoryCardBinding

class MemoryGridAdapter(
    private val onMemoryClick: (MemoryItem) -> Unit,
    private val onActionClick: (MemoryItem) -> Unit,
    private val onMemoryLongClick: (MemoryItem) -> Boolean = { false },
    private val actionMode: CardActionMode = CardActionMode.FAVORITE,
) : ListAdapter<MemoryItem, MemoryGridAdapter.MemoryViewHolder>(MemoryDiff) {
    private var selectedIds: Set<Long> = emptySet()

    fun setSelectedIds(ids: Set<Long>) {
        val next = ids.toSet()
        if (selectedIds == next) return
        selectedIds = next
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val binding = ItemMemoryCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return MemoryViewHolder(binding, onMemoryClick, onActionClick, onMemoryLongClick, actionMode)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, selectedIds.contains(item.id))
    }

    enum class CardActionMode {
        FAVORITE,
        DELETE,
    }

    class MemoryViewHolder(
        private val binding: ItemMemoryCardBinding,
        private val onMemoryClick: (MemoryItem) -> Unit,
        private val onActionClick: (MemoryItem) -> Unit,
        private val onMemoryLongClick: (MemoryItem) -> Boolean,
        private val actionMode: CardActionMode,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MemoryItem, selected: Boolean) = with(binding) {
            root.setOnClickListener { onMemoryClick(item) }
            root.setOnLongClickListener { onMemoryLongClick(item) }
            favoriteButton.setOnClickListener { onActionClick(item) }
            // 최대 2개 카테고리를 행을 구분해 표시한다.
            categoryBadge.text = item.categories
                .take(MemoryCategory.MAX_PER_MEMORY)
                .joinToString("\n") { it.displayName }
                .ifBlank { MemoryCategory.OTHERS.displayName }
            memoText.text = item.memo
            memoText.visibility = if (item.memo.isBlank()) View.GONE else View.VISIBLE
            timeText.text = DateUtils.getRelativeTimeSpanString(
                item.createdAtMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            )
            tagText.text = item.tags.firstOrNull().orEmpty()
            statusBadge.text = item.processingStatus.displayText()
            statusBadge.setBackgroundResource(item.processingStatus.badgeBackground())
            favoriteButton.setImageResource(actionMode.iconRes())
            favoriteButton.contentDescription = actionMode.contentDescription()
            ImageViewCompat.setImageTintList(
                favoriteButton,
                ContextCompat.getColorStateList(
                    favoriteButton.context,
                    actionMode.tintRes(item),
                ),
            )
            selectionOverlay.visibility = if (selected) View.VISIBLE else View.GONE
            selectionCheck.visibility = if (selected) View.VISIBLE else View.GONE
            root.strokeColor = ContextCompat.getColor(
                root.context,
                if (selected) R.color.snap_primary else R.color.snap_outline,
            )
            root.strokeWidth = root.resources.getDimensionPixelSize(
                if (selected) R.dimen.memory_card_selected_stroke else R.dimen.memory_card_default_stroke,
            )

            thumbFrame.setBackgroundResource(item.category.thumbnailBackground())
            if (item.imageUri.isNullOrBlank()) {
                thumbImage.setImageDrawable(null)
                thumbGlyph.text = item.category.glyph
            } else {
                thumbGlyph.text = ""
                Glide.with(thumbImage)
                    .load(Uri.parse(item.imageUri))
                    .thumbnail(0.25f)
                    .centerCrop()
                    .into(thumbImage)
            }
        }

        private fun ProcessingStatus.displayText(): String =
            when (this) {
                ProcessingStatus.PROCESSING -> "처리중"
                ProcessingStatus.DONE -> "완료"
                ProcessingStatus.ERROR -> "오류"
            }

        private fun ProcessingStatus.badgeBackground(): Int =
            when (this) {
                ProcessingStatus.PROCESSING -> R.drawable.bg_badge_amber
                ProcessingStatus.DONE -> R.drawable.bg_badge_primary
                ProcessingStatus.ERROR -> R.drawable.bg_badge_error
            }

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

        private fun CardActionMode.iconRes(): Int =
            when (this) {
                CardActionMode.FAVORITE -> R.drawable.ic_heart
                CardActionMode.DELETE -> R.drawable.ic_trash
            }

        private fun CardActionMode.contentDescription(): String =
            when (this) {
                CardActionMode.FAVORITE -> "즐겨찾기 토글"
                CardActionMode.DELETE -> "영구 삭제"
            }

        private fun CardActionMode.tintRes(item: MemoryItem): Int =
            when (this) {
                CardActionMode.FAVORITE -> if (item.isFavorite) R.color.snap_rose else R.color.snap_text_secondary
                CardActionMode.DELETE -> R.color.snap_rose
            }
    }

    private object MemoryDiff : DiffUtil.ItemCallback<MemoryItem>() {
        override fun areItemsTheSame(oldItem: MemoryItem, newItem: MemoryItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MemoryItem, newItem: MemoryItem): Boolean =
            oldItem == newItem
    }
}
