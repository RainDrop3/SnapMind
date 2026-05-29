package com.example.snapmind.feature.home

import android.net.Uri
import android.text.format.DateUtils
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
    private val onFavoriteClick: (MemoryItem) -> Unit,
) : ListAdapter<MemoryItem, MemoryGridAdapter.MemoryViewHolder>(MemoryDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val binding = ItemMemoryCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return MemoryViewHolder(binding, onMemoryClick, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MemoryViewHolder(
        private val binding: ItemMemoryCardBinding,
        private val onMemoryClick: (MemoryItem) -> Unit,
        private val onFavoriteClick: (MemoryItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MemoryItem) = with(binding) {
            root.setOnClickListener { onMemoryClick(item) }
            favoriteButton.setOnClickListener { onFavoriteClick(item) }
            categoryBadge.text = item.category.displayName
            memoText.text = item.memo.ifBlank { "메모가 아직 없어요. 상세 화면에서 저장 이유를 남겨보세요." }
            timeText.text = DateUtils.getRelativeTimeSpanString(
                item.createdAtMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            )
            tagText.text = item.tags.firstOrNull().orEmpty()
            statusBadge.text = item.processingStatus.displayText()
            statusBadge.setBackgroundResource(item.processingStatus.badgeBackground())
            ImageViewCompat.setImageTintList(
                favoriteButton,
                ContextCompat.getColorStateList(
                    favoriteButton.context,
                    if (item.isFavorite) R.color.snap_rose else R.color.snap_text_secondary,
                ),
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
                MemoryCategory.CODE -> R.drawable.bg_thumbnail_code
                MemoryCategory.SHOPPING -> R.drawable.bg_thumbnail_shopping
                MemoryCategory.MAP -> R.drawable.bg_thumbnail_map
                MemoryCategory.RECEIPT -> R.drawable.bg_thumbnail_receipt
                MemoryCategory.CHAT -> R.drawable.bg_thumbnail_chat
                MemoryCategory.YOUTUBE -> R.drawable.bg_thumbnail_youtube
                MemoryCategory.DOCUMENT,
                MemoryCategory.UNKNOWN -> R.drawable.bg_thumbnail_receipt
            }
    }

    private object MemoryDiff : DiffUtil.ItemCallback<MemoryItem>() {
        override fun areItemsTheSame(oldItem: MemoryItem, newItem: MemoryItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MemoryItem, newItem: MemoryItem): Boolean =
            oldItem == newItem
    }
}
